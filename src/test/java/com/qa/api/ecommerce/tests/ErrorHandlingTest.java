package com.qa.api.ecommerce.tests;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.ecommerce.base.ECommerceBaseTest;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;

@Epic("E-Commerce API")
@Story("09 Error Handling and Edge Cases")
@Severity(SeverityLevel.CRITICAL)
public class ErrorHandlingTest extends ECommerceBaseTest
{
	@Test
	public void unknownRouteReturns404()
	{
		assertNotFound(get("/does-not-exist"));
	}

	@Test
	public void unknownProductReturnsProductSpecific404()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/999999999");
		Assert.assertEquals(response.statusCode(), 404);
		Assert.assertEquals(response.jsonPath().getString("detail"), "Product not found");
	}

	@Test
	public void nonNumericProductIdReturns422IntParsing()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/abc");
		assertDetailArray(response);
		Assert.assertEquals(firstDetailType(response), "int_parsing");
		Assert.assertTrue(detailPaths(response).contains("path.product_id"), "loc is path.product_id");
		Assert.assertTrue(response.jsonPath().getString("detail[0].msg").toLowerCase().contains("valid integer"),
				"msg mentions a valid integer");
	}

	@Test
	@Description("FastAPI parse error returns 422 (json_invalid), not 400")
	public void malformedJsonBodyReturns422JsonInvalid()
	{
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT, "{ this is not valid json ]");
		Assert.assertEquals(response.statusCode(), 422, response.asString());
		Assert.assertEquals(firstDetailType(response), "json_invalid");
		Assert.assertEquals(response.jsonPath().getString("detail[0].loc[0]"), "body");
	}

	@Test
	public void bodyOfWrongJsonTypeReturns422()
	{
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT, Arrays.asList("not", "an", "object"));
		assertDetailArray(response);
	}

	@Test
	public void deleteProductReturns405()
	{
		assertMethodNotAllowed(delete(ECOMMERCE_PRODUCTS_ENDPOINT + "/999999999", null));
	}

	@Test
	public void deleteOrderReturns405()
	{
		assertMethodNotAllowed(delete(ECOMMERCE_ORDERS_ENDPOINT + "/999999999", null));
	}

	@Test
	public void purchasesWithoutUserIdReturns422MissingQuery()
	{
		Response response = get(ECOMMERCE_PURCHASES_ENDPOINT);
		assertDetailArray(response);
		Assert.assertTrue(detailPaths(response).contains("query.user_id"), "query.user_id flagged");
		Assert.assertEquals(firstDetailType(response), "missing");
	}

	@Test
	public void purchasesWithNonNumericUserIdReturns422IntParsing()
	{
		Response response = call("GET", ECOMMERCE_PURCHASES_ENDPOINT, null, null, null, query("user_id", "abc"));
		assertDetailArray(response);
		Assert.assertEquals(firstDetailType(response), "int_parsing");
		Assert.assertTrue(detailPaths(response).contains("query.user_id"), "loc is query.user_id");
	}

	@Test
	@Description("Error responses must not leak stack traces and must stay JSON")
	public void errorResponsesNeverLeakStackTraces()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/abc");
		String raw = response.asString();
		Assert.assertFalse(raw.matches("(?s).*(Traceback|File \"|stack|__traceback__).*"),
				"no stack trace or traceback in body: " + raw);
		Assert.assertTrue(response.contentType().contains("application/json"),
				"content-type is application/json on errors, was " + response.contentType());
	}
}
