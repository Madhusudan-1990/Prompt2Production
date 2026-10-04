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
@Story("02 Product Endpoints")
@Severity(SeverityLevel.CRITICAL)
public class ProductEndpointsTest extends ECommerceBaseTest
{
	@Test
	@Description("Baseline list used by pagination/sort/filter tests to prove query params are ignored")
	public void listProductsBaseline()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> list = response.jsonPath().getList("$");
		Assert.assertTrue(list.size() >= 1, "at least one product");
		for (Map<String, Object> p : list)
		{
			for (String key : new String[] { "id", "name", "price", "stock", "selected" })
			{
				Assert.assertTrue(p.containsKey(key), "product must have " + key);
			}
			Assert.assertFalse(p.containsKey("password"), "products must not expose password");
		}
		Assert.assertTrue(response.time() < 1000, "Response time < 1000ms, was " + response.time() + "ms");
	}

	@Test
	@Description("API returns 200, not 201, on create")
	public void createProductEchoesPayload()
	{
		Map<String, Object> body = json(
				"name", tcName("live"),
				"price", 12.34,
				"stock", 7,
				"selected", false);
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT, body);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.jsonPath().getString("name").startsWith("tc-live-"), "tc- tagged name");
		Assert.assertEquals((double) response.jsonPath().getFloat("price"), 12.34, 0.0001);
		Assert.assertEquals(response.jsonPath().getInt("stock"), 7);
		Assert.assertFalse(response.jsonPath().getBoolean("selected"));
	}

	@Test(dependsOnMethods = "createProductEchoesPayload")
	public void getProductById()
	{
		String id = ensureProductId();
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getInt("id"), Integer.parseInt(id));
		Assert.assertTrue(response.jsonPath().getString("name").startsWith("tc-"), "created fixture product");
	}

	@Test(dependsOnMethods = "getProductById")
	@Description("PUT is implemented even though the OpenAPI spec only advertises GET/POST for this path")
	public void putUpdatesEntireProduct()
	{
		String id = ensureProductId();
		Map<String, Object> body = json(
				"name", tcName("live-updated"),
				"price", 99.5,
				"stock", 3,
				"selected", true);
		Response putResponse = put(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id, body, null);
		Assert.assertEquals(putResponse.statusCode(), 200);

		Response verify = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id);
		Assert.assertEquals(verify.statusCode(), 200);
		Assert.assertEquals((double) verify.jsonPath().getFloat("price"), 99.5, 0.0001);
		Assert.assertEquals(verify.jsonPath().getInt("stock"), 3);
		Assert.assertTrue(verify.jsonPath().getString("name").startsWith("tc-live-updated-"));
		//FINDING: PUT ignores the 'selected' field - only PATCH /products/{id}/select changes it
		Assert.assertFalse(verify.jsonPath().getBoolean("selected"),
				"FINDING: PUT must ignore 'selected' (flag stays false)");
	}

	@Test(dependsOnMethods = "putUpdatesEntireProduct")
	public void putWithoutBodyReturns422()
	{
		String id = ensureProductId();
		Response response = put(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id, null, null);
		assertDetailArray(response);
		Assert.assertTrue(detailPaths(response).contains("body"), "body reported missing");
		Assert.assertEquals(firstDetailType(response), "missing");
	}

	@Test(dependsOnMethods = "putWithoutBodyReturns422")
	@Description("Prompt assumed PATCH support; the API does not implement it")
	public void patchProductReturns405()
	{
		String id = ensureProductId();
		Response response = patch(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id, json("price", 1), null);
		assertMethodNotAllowed(response);
	}

	@Test(dependsOnMethods = "patchProductReturns405")
	@Description("Prompt assumed DELETE support; it returns 405 and the product survives")
	public void deleteProductReturns405AndProductSurvives()
	{
		String id = ensureProductId();
		Response response = delete(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id, null);
		assertMethodNotAllowed(response);
		Assert.assertEquals(get(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id).statusCode(), 200,
				"product still exists after DELETE attempt");
	}

	@Test(dependsOnMethods = "deleteProductReturns405AndProductSurvives")
	public void patchSelectTogglesSelectedFlag()
	{
		String id = ensureProductId();
		Response response = patch(ECOMMERCE_PRODUCT_SELECT_ENDPOINT, json("selected", true), path("id", id));
		Assert.assertEquals(response.statusCode(), 200);
		Response verify = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/" + id);
		Assert.assertTrue(verify.jsonPath().getBoolean("selected"), "selected flag updated");
	}

	@Test
	public void getUnknownProductReturns404()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT + "/999999999");
		Assert.assertEquals(response.statusCode(), 404);
		Assert.assertEquals(response.jsonPath().getString("detail"), "Product not found");
	}

	@Test
	@Description("FastAPI validation error: missing required fields")
	public void createProductWithMissingFieldsReturns422()
	{
		Response response = post(ECOMMERCE_PRODUCTS_ENDPOINT, json("price", 5));
		assertDetailArray(response);
		List<String> locs = detailPaths(response);
		Assert.assertTrue(locs.contains("body.name"), "body.name flagged missing");
		Assert.assertTrue(locs.contains("body.selected"), "body.selected flagged missing");
	}
}
