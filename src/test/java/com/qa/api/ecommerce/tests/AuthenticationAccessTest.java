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
@Story("01 Authentication & Access")
@Severity(SeverityLevel.BLOCKER)
public class AuthenticationAccessTest extends ECommerceBaseTest
{
	@Test
	public void getProductsWithoutAnyAuth()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertNotNull(response.jsonPath().getList("$"), "array expected without credentials");
		Assert.assertNull(response.header("WWW-Authenticate"), "no auth challenge expected");
	}

	@Test
	@Description("No 401 is returned: API has no auth middleware")
	public void getProductsWithBearerTokenIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null,
				headers("Authorization", "Bearer definitely-not-a-valid-token"), null, null);
		Assert.assertEquals(response.statusCode(), 200, "auth header is ignored - still 200");
		Assert.assertNotNull(response.jsonPath().getList("$"));
	}

	@Test
	public void openapiSpecDeclaresNoSecuritySchemes()
	{
		Response response = get(ECOMMERCE_OPENAPI_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);

		Object schemes = response.jsonPath().get("components.securitySchemes");
		Assert.assertTrue(schemes == null
				|| (schemes instanceof Map && ((Map<?, ?>) schemes).isEmpty()),
				"securitySchemes must be absent or empty but was: " + schemes);
		Assert.assertNull(response.jsonPath().get("security"), "root security must be undefined");

		Map<?, ?> paths = response.jsonPath().getMap("paths");
		for (String path : new String[] { "/products", "/orders", "/users", "/purchases" })
		{
			Assert.assertTrue(paths.containsKey(path), "spec must document " + path);
		}
	}

	@Test
	public void healthEndpointIsPubliclyReachable()
	{
		Response response = get(ECOMMERCE_HEALTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getString("status"), "ok");
		String timestamp = response.jsonPath().getString("timestamp");
		Assert.assertNotNull(java.time.LocalDateTime.parse(timestamp), "ISO 8601 timestamp: " + timestamp);
	}

	@Test
	@Description("/admin/stats requires no credentials - access-control finding")
	public void adminStatsNeedsNoAuth()
	{
		Response response = get(ECOMMERCE_ADMIN_STATS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		for (String key : new String[] { "total_products", "total_orders", "total_users" })
		{
			Number value = response.jsonPath().get(key);
			Assert.assertNotNull(value, key + " present");
			Assert.assertTrue(value.doubleValue() >= 0, key + " is a non-negative number");
		}
	}

	@Test
	@Description("Debug endpoint is publicly reachable - noted as an access-control finding")
	public void debugDbIsPubliclyReachable()
	{
		Response response = get(ECOMMERCE_DEBUG_DB_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertNotNull(response.jsonPath().getList("products"), "products collection array");
		Assert.assertNotNull(response.jsonPath().get("orders"), "orders collection");
	}
}
