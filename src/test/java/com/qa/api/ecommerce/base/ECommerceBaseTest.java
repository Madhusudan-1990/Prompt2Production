package com.qa.api.ecommerce.base;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Base class for the e-commerce API suite.
 *
 * The live API (https://ecommerce-api.fastapicloud.dev) has NO authentication and NO delete endpoints,
 * so test data cannot be cleaned up - every resource this suite creates is tagged with a "tc-" prefix
 * to keep it recognisable on the shared demo server.
 *
 * Test data flows forward through the suite (product -> user -> order) via the lazy fixtures below,
 * which behave like the Postman collection's ENSURE pre-request script: create once per run, reuse after.
 */
public class ECommerceBaseTest extends BaseTest
{
	private static final AtomicInteger SEQ = new AtomicInteger(1);
	private static final int TRANSIENT_RETRIES = 3;
	private static volatile String productId;
	private static volatile String userId;
	private static volatile String orderId;

	//Seeded product id 1 always exists and is the only product id that is safe for order creation
	//(see OrderManagementTest: the order handler indexes the product list with the raw product id).
	protected static final int SEEDED_PRODUCT_ID = 1;

	/** Unique tc- prefixed name so created data is identifiable on the shared server. */
	protected String tcName(String prefix)
	{
		return "tc-" + prefix + "-" + System.currentTimeMillis() + "-" + SEQ.getAndIncrement();
	}

	protected String tcEmail()
	{
		return "tc-" + System.currentTimeMillis() + "-" + SEQ.getAndIncrement() + "@example.test";
	}

	/** Spec-free call used by every e-commerce test - the test itself owns the expected status code.
	 *  Retries 503 responses a few times: the target is a shared public demo that occasionally restarts. */
	protected Response call(String method, String endpoint, Object body,
			Map<String, String> headers, Map<String, String> pathParams, Map<String, String> queryParams)
	{
		Response response = null;
		for (int attempt = 1; attempt <= TRANSIENT_RETRIES; attempt++)
		{
			response = restClient.execute(method, BASE_URL_ECOMMERCE, endpoint, body, headers, queryParams,
					pathParams, AuthType.NO_AUTH, ContentType.JSON);
			if (response.statusCode() != 503)
			{
				return response;
			}
			System.out.println("Transient 503 from shared API - attempt " + attempt + "/" + TRANSIENT_RETRIES);
			try
			{
				Thread.sleep(1000L * attempt);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return response;
			}
		}
		return response;
	}

	protected Response get(String endpoint)
	{
		return call("GET", endpoint, null, null, null, null);
	}

	protected Response post(String endpoint, Object body)
	{
		return call("POST", endpoint, body, null, null, null);
	}

	protected Response put(String endpoint, Object body, Map<String, String> pathParams)
	{
		return call("PUT", endpoint, body, null, pathParams, null);
	}

	protected Response patch(String endpoint, Object body, Map<String, String> pathParams)
	{
		return call("PATCH", endpoint, body, null, pathParams, null);
	}

	protected Response delete(String endpoint, Map<String, String> pathParams)
	{
		return call("DELETE", endpoint, null, null, pathParams, null);
	}

	protected Map<String, Object> json(Object... kv)
	{
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2)
		{
			map.put((String) kv[i], kv[i + 1]);
		}
		return map;
	}

	protected Map<String, String> path(String key, Object value)
	{
		Map<String, String> map = new HashMap<>();
		map.put(key, String.valueOf(value));
		return map;
	}

	protected Map<String, String> headers(String... kv)
	{
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2)
		{
			map.put(kv[i], kv[i + 1]);
		}
		return map;
	}

	protected Map<String, String> query(String... kv)
	{
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2)
		{
			map.put(kv[i], kv[i + 1]);
		}
		return map;
	}

	/** Asserts the FastAPI 422 error contract: detail is an array of {loc, msg, type}. */
	protected void assertDetailArray(Response response)
	{
		Assert.assertEquals(response.statusCode(), 422, "expected FastAPI validation error: " + response.asString());
		Assert.assertNotNull(response.jsonPath().getList("detail"), "detail array");
		for (Object entry : response.jsonPath().getList("detail"))
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> error = (Map<String, Object>) entry;
			Assert.assertTrue(error.containsKey("loc"), "error must have loc");
			Assert.assertTrue(error.containsKey("msg"), "error must have msg");
			Assert.assertTrue(error.containsKey("type"), "error must have type");
		}
	}

	/** Returns each 422 error location as a dotted path, e.g. "body.name", "query.user_id". */
	@SuppressWarnings("unchecked")
	protected java.util.List<String> detailPaths(Response response)
	{
		java.util.List<String> paths = new java.util.ArrayList<>();
		for (Object entry : response.jsonPath().getList("detail"))
		{
			Map<String, Object> error = (Map<String, Object>) entry;
			Object loc = error.get("loc");
			StringBuilder sb = new StringBuilder();
			if (loc instanceof java.util.List)
			{
				for (Object part : (java.util.List<?>) loc)
				{
					if (sb.length() > 0)
					{
						sb.append('.');
					}
					sb.append(part);
				}
			}
			else
			{
				sb.append(loc);
			}
			paths.add(sb.toString());
		}
		return paths;
	}

	/** type field of the first 422 error. */
	@SuppressWarnings("unchecked")
	protected String firstDetailType(Response response)
	{
		Map<String, Object> first = (Map<String, Object>) response.jsonPath().getList("detail").get(0);
		return String.valueOf(first.get("type"));
	}

	/** FastAPI 405 contract: {"detail": "Method Not Allowed"}. */
	protected void assertMethodNotAllowed(Response response)
	{
		Assert.assertEquals(response.statusCode(), 405, response.asString());
		Assert.assertEquals(response.jsonPath().getString("detail"), "Method Not Allowed");
	}

	/** FastAPI 404 contract: {"detail": "<string>"}. */
	protected void assertNotFound(Response response)
	{
		Assert.assertEquals(response.statusCode(), 404, response.asString());
		Assert.assertNotNull(response.jsonPath().getString("detail"), "404 detail string");
	}

	/** Lazily creates (once per run) the tc- product used by dependent tests. */
	protected synchronized String ensureProductId()
	{
		if (productId != null)
		{
			return productId;
		}
		Map<String, Object> body = json(
				"name", tcName("product"),
				"price", 49.99,
				"stock", 10,
				"selected", false);
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT, body);
		Assert.assertEquals(response.statusCode(), 200, "fixture product creation failed: " + response.asString());
		productId = String.valueOf(response.jsonPath().getInt("id"));
		return productId;
	}

	/** Lazily creates (once per run) the tc- user used by dependent tests. */
	protected synchronized String ensureUserId()
	{
		if (userId != null)
		{
			return userId;
		}
		Map<String, Object> body = json("name", tcName("user"), "email", tcEmail());
		Response response = post(ECOMMERCE_USERS_ENDPOINT, body);
		Assert.assertEquals(response.statusCode(), 200, "fixture user creation failed: " + response.asString());
		userId = String.valueOf(response.jsonPath().getInt("id"));
		return userId;
	}

	/**
	 * Lazily creates (once per run) the order used by dependent tests.
	 * Uses the seeded product id 1 - arbitrary product ids can crash the order handler (see validation tests).
	 */
	protected synchronized String ensureOrderId()
	{
		if (orderId != null)
		{
			return orderId;
		}
		Map<String, Object> body = json(
				"user_id", Integer.parseInt(ensureUserId()),
				"product_ids", new int[] { SEEDED_PRODUCT_ID },
				"quantities", new int[] { 1 });
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT, body);
		Assert.assertEquals(response.statusCode(), 200, "fixture order creation failed: " + response.asString());
		orderId = String.valueOf(response.jsonPath().getInt("id"));
		return orderId;
	}
}
