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
@Story("08 Search and Filter")
@Severity(SeverityLevel.NORMAL)
public class SearchGapTest extends ECommerceBaseTest
{
	@Test
	public void searchEndpointIsCapturedByProductIdRoute()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT + "/search", null, null, null,
				query("q", "laptop"));
		assertDetailArray(response);
		Assert.assertEquals(firstDetailType(response), "int_parsing");
		Assert.assertTrue(detailPaths(response).contains("path.product_id"), "'search' parsed as product_id");
		Assert.assertEquals(response.jsonPath().getString("detail[0].input"), "search");
	}

	@Test
	public void filterEndpointIsCapturedByProductIdRoute()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT + "/filter", null, null, null,
				query("minPrice", "10", "maxPrice", "100"));
		assertDetailArray(response);
		Assert.assertEquals(response.jsonPath().getString("detail[0].input"), "filter");
	}

	@Test
	@Description("FINDING: /products ignores q, minPrice, maxPrice and category parameters")
	public void searchParamsOnProductsAreIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("q", "laptop", "minPrice", "10", "maxPrice", "100", "category", "electronics"));
		Assert.assertEquals(response.statusCode(), 200);

		List<Object> baseline = get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id");
		List<Object> filtered = response.jsonPath().getList("id");
		Assert.assertEquals(filtered, baseline, "returns the unfiltered product list (params ignored)");
	}

	@Test
	public void extraParamsOnProductByIdAreIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT + "/" + SEEDED_PRODUCT_ID, null, null, null,
				query("q", "ignored", "fields", "name"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getInt("id"), SEEDED_PRODUCT_ID,
				"path resource returned, extra params ignored");
	}
}
