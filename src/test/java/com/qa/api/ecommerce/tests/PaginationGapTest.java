package com.qa.api.ecommerce.tests;

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
@Story("11 Pagination and Data Limits")
@Severity(SeverityLevel.NORMAL)
public class PaginationGapTest extends ECommerceBaseTest
{
	@Test
	@Description("FINDING: no pagination - limit param ignored")
	public void limitParamIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null, query("limit", "1"));
		Assert.assertEquals(response.statusCode(), 200);
		List<Object> baseline = get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id");
		List<Object> data = response.jsonPath().getList("id");
		Assert.assertEquals(data.size(), baseline.size(), "limit=1 is ignored (full list returned)");
		Assert.assertTrue(data.size() > 1, "more than one product exists");
	}

	@Test
	public void outOfRangePageIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("page", "99999", "limit", "10"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.jsonPath().getList("$").size() > 0,
				"out-of-range page ignored, results not empty");
	}

	@Test
	public void skipTakeParamsAreIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("skip", "0", "take", "1"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id"),
				get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id"), "skip/take ignored");
	}

	@Test
	@Description("FINDING: query params are never validated or typed")
	public void nonNumericLimitIsAccepted()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null, query("limit", "abc"));
		Assert.assertEquals(response.statusCode(), 200, "unknown params are not validated");
		Assert.assertNotNull(response.jsonPath().getList("$"), "non-numeric limit accepted");
	}

	@Test
	public void ordersSkipTakeParamsAreIgnored()
	{
		Response response = call("GET", ECOMMERCE_ORDERS_ENDPOINT, null, null, null,
				query("skip", "0", "take", "5"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id"),
				get(ECOMMERCE_ORDERS_ENDPOINT).jsonPath().getList("id"), "orders skip/take ignored");
	}

	@Test
	@Description("FINDING: response is a bare array - no {total, page, limit} envelope")
	public void responseIsBareArrayWithoutEnvelope()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("page", "1", "limit", "10"));
		Assert.assertEquals(response.statusCode(), 200);
		String raw = response.asString().trim();
		Assert.assertTrue(raw.startsWith("["), "top-level array, not an envelope: " + raw.substring(0, 40));
	}
}
