package com.qa.api.ecommerce.tests;

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
@Story("10 HTTP Methods and CORS (OPTIONS)")
@Severity(SeverityLevel.CRITICAL)
public class CorsOptionsTest extends ECommerceBaseTest
{
	private static final String ORIGIN = "https://example.com";

	private Response preflight(String endpoint, boolean sendOrigin, boolean sendAcrm, String requestHeaders)
	{
		Map<String, String> headers = new java.util.HashMap<>();
		if (sendOrigin)
		{
			headers.put("Origin", ORIGIN);
		}
		if (sendAcrm)
		{
			headers.put("Access-Control-Request-Method", "POST");
		}
		if (requestHeaders != null)
		{
			headers.put("Access-Control-Request-Headers", requestHeaders);
		}
		return call("OPTIONS", endpoint, null, headers.isEmpty() ? null : headers, null, null);
	}

	private void assertPreflightCorsHeaders(Response response)
	{
		Assert.assertEquals(response.statusCode(), 200, response.asString());
		String methods = response.header("Access-Control-Allow-Methods");
		Assert.assertNotNull(methods, "ACAM present");
		for (String method : new String[] { "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD" })
		{
			Assert.assertTrue(methods.toUpperCase().contains(method), "ACAM lists " + method);
		}
		Assert.assertEquals(response.header("Access-Control-Allow-Origin"), ORIGIN, "ACAO reflects origin");
		Assert.assertEquals(response.header("Access-Control-Allow-Credentials"), "true", "ACAC true");
		Assert.assertNotNull(response.header("Access-Control-Max-Age"), "max-age present");
	}

	@Test
	public void optionsProductsPreflightReturns200WithCorsHeaders()
	{
		assertPreflightCorsHeaders(preflight(ECOMMERCE_PRODUCTS_ENDPOINT, true, true, null));
	}

	@Test
	public void optionsWithoutOriginReturns405()
	{
		assertMethodNotAllowed(preflight(ECOMMERCE_PRODUCTS_ENDPOINT, false, false, null));
	}

	@Test
	public void optionsWithOriginOnlyNoRequestMethodReturns405()
	{
		assertMethodNotAllowed(preflight(ECOMMERCE_PRODUCTS_ENDPOINT, true, false, null));
	}

	@Test
	public void optionsOrdersPreflightReturns200WithCorsHeaders()
	{
		assertPreflightCorsHeaders(preflight(ECOMMERCE_ORDERS_ENDPOINT, true, true, null));
	}

	@Test
	public void optionsUsersPreflightReturns200WithCorsHeaders()
	{
		assertPreflightCorsHeaders(preflight(ECOMMERCE_USERS_ENDPOINT, true, true, null));
	}

	@Test
	public void optionsPurchasesPreflightReturns200WithCorsHeaders()
	{
		assertPreflightCorsHeaders(preflight(ECOMMERCE_PURCHASES_ENDPOINT, true, true, null));
	}

	@Test
	@Description("CORS middleware answers any path, even non-existent API paths")
	public void optionsPaymentsPreflightReturns200()
	{
		Response response = preflight(ECOMMERCE_PAYMENTS_ENDPOINT, true, true, null);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.header("Access-Control-Allow-Origin"), ORIGIN,
				"ACAO present even for non-existent API path");
	}

	@Test
	public void optionsUnknownRoutePreflightReturns200()
	{
		Response response = preflight("/auth/login", true, true, null);
		Assert.assertEquals(response.statusCode(), 200, "no such route, CORS still answers");
	}

	@Test
	@Description("Access-Control-Allow-Headers is set when requested")
	public void allowHeadersEchoRequestedHeaders()
	{
		Response response = preflight(ECOMMERCE_PRODUCTS_ENDPOINT, true, true, "content-type,authorization");
		Assert.assertEquals(response.statusCode(), 200, "preflight succeeds for the requested headers");
		String allowed = response.header("Access-Control-Allow-Headers");
		Assert.assertNotNull(allowed, "ACAH present");
		String lower = allowed.toLowerCase();
		Assert.assertTrue(lower.contains("content-type"), "ACAH authorises content-type, was: " + lower);
		Assert.assertTrue(lower.contains("authorization"), "ACAH authorises authorization, was: " + lower);
	}

	@Test
	public void simpleGetWithOriginGetsAcao()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null,
				headers("Origin", ORIGIN), null, null);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.header("Access-Control-Allow-Origin"), ORIGIN, "ACAO on simple requests");
		Assert.assertEquals(response.header("Access-Control-Allow-Credentials"), "true", "ACAC echoed");
	}
}
