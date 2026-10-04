package com.qa.api.ecommerce.tests;

import java.util.HashSet;
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
@Story("04 Order Management")
@Severity(SeverityLevel.CRITICAL)
public class OrderManagementTest extends ECommerceBaseTest
{
	@Test
	public void listOrdersBaseline()
	{
		Response response = get(ECOMMERCE_ORDERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> list = response.jsonPath().getList("$");
		Assert.assertTrue(list.size() >= 1, "seeded orders present");
		Map<String, Object> order = list.get(0);
		for (String key : new String[] { "id", "customer_id", "order_date", "total_cost", "items" })
		{
			Assert.assertTrue(order.containsKey(key), "order must have " + key);
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
		Assert.assertTrue(items.get(0).containsKey("product_id"), "items[].product_id");
		Assert.assertTrue(items.get(0).containsKey("quantity"), "items[].quantity");
		Assert.assertTrue(response.time() < 1000, "Response time < 1000ms, was " + response.time() + "ms");
	}

	@Test
	@Description("Uses seeded product id 1 - product ids >= product count crash the handler with 500")
	public void createOrderReturns200WithPendingStatus()
	{
		Map<String, Object> body = json(
				"user_id", Integer.parseInt(ensureUserId()),
				"product_ids", new int[] { SEEDED_PRODUCT_ID },
				"quantities", new int[] { 1 });
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT, body);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertNotNull(response.jsonPath().get("id"), "id");
		Assert.assertEquals(response.jsonPath().getString("status"), "pending");
		Assert.assertNotNull(response.jsonPath().getDouble("total_cost"), "total_cost");
		Assert.assertNotNull(response.jsonPath().getString("created_at"), "ISO created_at");
	}

	@Test(dependsOnMethods = "createOrderReturns200WithPendingStatus")
	@Description("FINDING: the handler indexes the product list with the raw product id (off-by-one)")
	public void orderTotalCostUsesProductListIndex()
	{
		Map<String, Object> body = json(
				"user_id", Integer.parseInt(ensureUserId()),
				"product_ids", new int[] { SEEDED_PRODUCT_ID },
				"quantities", new int[] { 1 });
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT, body);
		Assert.assertEquals(response.statusCode(), 200);

		List<Map<String, Object>> products = get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("$");
		Map<String, Object> byIndex = products.get(SEEDED_PRODUCT_ID); //0-based index == product id
		Map<String, Object> byId = products.stream()
				.filter(p -> ((Number) p.get("id")).intValue() == SEEDED_PRODUCT_ID)
				.findFirst().orElse(null);

		double total = response.jsonPath().getDouble("total_cost");
		Assert.assertEquals(total, ((Number) byIndex.get("price")).doubleValue(), 0.0001,
				"total_cost equals products[product_id] price (off-by-one)");
		Assert.assertTrue(byId != null && ((Number) byIndex.get("id")).intValue() != SEEDED_PRODUCT_ID,
				"FINDING: order handler looks up products by list index, not by product id");
	}

	@Test(dependsOnMethods = "createOrderReturns200WithPendingStatus")
	public void listOrdersContainsCreatedOrderWithUniqueIds()
	{
		String id = ensureOrderId();
		Response response = get(ECOMMERCE_ORDERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		List<String> ids = response.jsonPath().getList("id", String.class);
		Assert.assertEquals(new HashSet<>(ids).size(), ids.size(), "ids remain unique");
		Assert.assertTrue(ids.contains(id), "created order present in list");
	}

	@Test
	@Description("FastAPI validation error: missing required field")
	public void createOrderMissingUserIdReturns422()
	{
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT,
				json("product_ids", new int[] { 1 }, "quantities", new int[] { 1 }));
		assertDetailArray(response);
		Assert.assertTrue(detailPaths(response).contains("body.user_id"), "body.user_id flagged");
	}

	@Test
	public void createOrderWithWrongUserIdTypeReturns422IntParsing()
	{
		Response response = post(ECOMMERCE_ORDERS_ENDPOINT,
				json("user_id", "abc", "product_ids", new int[] {}, "quantities", new int[] {}));
		assertDetailArray(response);
		Assert.assertEquals(firstDetailType(response), "int_parsing");
		Assert.assertTrue(detailPaths(response).contains("body.user_id"), "loc is body.user_id");
	}

	@Test
	@Description("Prompt assumed GET /orders/{id}; it always 404s even for existing orders")
	public void getOrderByIdReturns404()
	{
		Response response = get(ECOMMERCE_ORDERS_ENDPOINT + "/" + ensureOrderId());
		assertNotFound(response);
	}

	@Test
	public void putOrderReturns405()
	{
		String id = ensureOrderId();
		Response response = put(ECOMMERCE_ORDERS_ENDPOINT + "/" + id,
				json("user_id", 1, "product_ids", new int[] { 1 }, "quantities", new int[] { 1 }),
				null);
		assertMethodNotAllowed(response);
	}

	@Test
	@Description("No order status updates are possible - status is set to 'pending' by the server")
	public void patchOrderReturns405()
	{
		String id = ensureOrderId();
		Response response = patch(ECOMMERCE_ORDERS_ENDPOINT + "/" + id, json("status", "shipped"), null);
		assertMethodNotAllowed(response);
	}

	@Test
	public void deleteOrderReturns405AndOrderSurvives()
	{
		String id = ensureOrderId();
		Response response = delete(ECOMMERCE_ORDERS_ENDPOINT + "/" + id, null);
		assertMethodNotAllowed(response);
		Assert.assertTrue(get(ECOMMERCE_ORDERS_ENDPOINT).jsonPath().getList("id", String.class).contains(id),
				"order still present after DELETE attempt");
	}
}
