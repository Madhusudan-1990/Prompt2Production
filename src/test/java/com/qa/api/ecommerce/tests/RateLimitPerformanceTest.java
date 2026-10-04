package com.qa.api.ecommerce.tests;

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
@Story("15 Rate Limiting and Performance")
@Severity(SeverityLevel.NORMAL)
public class RateLimitPerformanceTest extends ECommerceBaseTest
{
	private void assertFastAndOk(Response response)
	{
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.time() < 1000,
				"under 1000ms, was " + response.time() + "ms");
	}

	@Test
	public void productsResponseTime()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT);
		assertFastAndOk(response);
		System.out.println("/products -> " + response.time() + "ms, payload " + response.asString().length()
				+ " bytes");
	}

	@Test
	public void ordersResponseTime()
	{
		Response response = get(ECOMMERCE_ORDERS_ENDPOINT);
		assertFastAndOk(response);
		System.out.println("/orders -> " + response.time() + "ms");
	}

	@Test
	public void purchasesResponseTime()
	{
		assertFastAndOk(call("GET", ECOMMERCE_PURCHASES_ENDPOINT, null, null, null,
				query("user_id", "1")));
	}

	@Test
	public void adminStatsResponseTime()
	{
		assertFastAndOk(get(ECOMMERCE_ADMIN_STATS_ENDPOINT));
	}

	@Test
	@Description("Deliberately gentle (12 calls) - this is a shared public demo, not a load test")
	public void burstOfTwelveRapidRequestsNeverReturns429()
	{
		Response baseline = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null, query("limit", "1"));
		Assert.assertEquals(baseline.statusCode(), 200, "baseline 200");

		StringBuilder codes = new StringBuilder();
		for (int i = 0; i < 12; i++)
		{
			int code = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null, null).statusCode();
			codes.append(code).append(' ');
			Assert.assertNotEquals(code, 429, "no 429 Too Many Requests across 12 rapid calls");
		}
		System.out.println("burst codes: " + codes);
	}

	@Test
	@Description("FINDING: API exposes no rate limiting")
	public void noRateLimitHeadersAreExposed()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT);
		Assert.assertNull(response.header("X-RateLimit-Limit"), "X-RateLimit-Limit absent");
		Assert.assertNull(response.header("X-RateLimit-Remaining"), "X-RateLimit-Remaining absent");
		Assert.assertNull(response.header("Retry-After"), "Retry-After absent");
	}

	@Test
	@Description("FINDING: Access-Control-Allow-Methods advertises HEAD but HEAD /products returns 404")
	public void headProductsReturns404()
	{
		Response response = call("HEAD", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null, null);
		Assert.assertEquals(response.statusCode(), 404, "HEAD not routed");
	}
}
