package com.qa.api.ecommerce.tests;

import java.util.List;
import java.util.Map;

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
@Story("12 Data Validation and Constraints")
@Severity(SeverityLevel.CRITICAL)
public class ValidationTest extends ECommerceBaseTest
{
	@Test
	public void emptyProductBodyReturns422WithPerFieldMessages()
	{
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT, json());
		assertDetailArray(response);

		Map<String, String> messagesByField = new java.util.HashMap<>();
		for (Object entry : response.jsonPath().getList("detail"))
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> error = (Map<String, Object>) entry;
			StringBuilder loc = new StringBuilder();
			for (Object part : (List<?>) error.get("loc"))
			{
				if (loc.length() > 0)
				{
					loc.append('.');
				}
				loc.append(part);
			}
			messagesByField.put(loc.toString(), String.valueOf(error.get("msg")));
		}
		Assert.assertTrue(messagesByField.getOrDefault("body.name", "").length() > 0, "body.name has a message");
		Assert.assertTrue(messagesByField.getOrDefault("body.selected", "").length() > 0,
				"body.selected has a message");
	}

	@Test
	public void wrongFieldTypesReturn422PerField()
	{
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT,
				json("name", 123, "price", "abc", "selected", "yes", "stock", "ten"));
		assertDetailArray(response);

		Map<String, String> typesByField = new java.util.HashMap<>();
		for (Object entry : response.jsonPath().getList("detail"))
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> error = (Map<String, Object>) entry;
			StringBuilder loc = new StringBuilder();
			for (Object part : (List<?>) error.get("loc"))
			{
				if (loc.length() > 0)
				{
					loc.append('.');
				}
				loc.append(part);
			}
			typesByField.put(loc.toString(), String.valueOf(error.get("type")));
		}
		Assert.assertEquals(typesByField.get("body.name"), "string_type");
		Assert.assertEquals(typesByField.get("body.price"), "float_parsing");
	}

	@Test
	@Description("Creates a tc-tagged junk product to prove the gap")
	public void emptyNameAndNegativePriceAreAccepted()
	{
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT,
				json("name", "", "price", -999.99, "stock", -5, "selected", false));
		Assert.assertEquals(response.statusCode(), 200, "no length/range validation");
		Assert.assertEquals(response.jsonPath().getString("name"), "", "FINDING: empty name accepted");
		Assert.assertTrue(response.jsonPath().getFloat("price") < 0, "FINDING: negative price accepted");
		Assert.assertTrue(response.jsonPath().getInt("stock") < 0, "FINDING: negative stock accepted");
	}

	@Test
	@Description("FINDING: no email format validation")
	public void blankNameAndBadEmailAreAccepted()
	{
		Response response = post(ECOMMERCE_USERS_ENDPOINT,
				json("name", "", "email", "definitely-not-an-email"));
		Assert.assertEquals(response.statusCode(), 200, "no format validation");
		Assert.assertEquals(response.jsonPath().getString("email"), "definitely-not-an-email",
				"invalid email stored as-is");
	}

	@Test
	public void orderMissingArrayFieldsReturns422()
	{
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT, json("user_id", 1));
		assertDetailArray(response);
		List<String> locs = detailPaths(response);
		Assert.assertTrue(locs.contains("body.product_ids"), "body.product_ids flagged");
		Assert.assertTrue(locs.contains("body.quantities"), "body.quantities flagged");
	}

	@Test
	@Description("BUG: product ids are used as list indexes - out-of-range ids raise instead of returning 4xx")
	public void orderWithOutOfRangeProductIdReturns500()
	{
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT,
				json("user_id", 1, "product_ids", new int[] { 999999 }, "quantities", new int[] { 1 }));
		Assert.assertEquals(response.statusCode(), 500,
				"FINDING: handler crashes on unknown product id instead of 422/404");
		Assert.assertFalse(response.contentType().contains("application/json"),
				"body is plain text, not a JSON error, was: " + response.contentType());
	}

	@Test
	@Description("Pydantic default config ignores extra body fields")
	public void orderExtraFieldsAreIgnored()
	{
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT,
				json("user_id", 1,
						"product_ids", new int[] { 1 },
						"quantities", new int[] { 1 },
						"coupon", "FREE",
						"notes", "x"));
		Assert.assertEquals(response.statusCode(), 200, "extra fields silently ignored");
		Map<String, Object> body = response.jsonPath().getMap("$");
		Assert.assertFalse(body.containsKey("coupon"), "no coupon echo");
		Assert.assertFalse(body.containsKey("notes"), "no notes echo");
	}
}
