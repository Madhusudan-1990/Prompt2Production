package com.qa.api.ecommerce.tests;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@Story("03 User / Customer Endpoints")
@Severity(SeverityLevel.CRITICAL)
public class UserEndpointsTest extends ECommerceBaseTest
{
	@Test
	public void listUsers()
	{
		Response response = get(ECOMMERCE_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> list = response.jsonPath().getList("$");
		Assert.assertNotNull(list, "array of users");
		for (Map<String, Object> user : list)
		{
			Assert.assertTrue(user.containsKey("id"), "id");
			Assert.assertTrue(user.containsKey("name"), "name");
			Assert.assertTrue(user.containsKey("email"), "email");
			Assert.assertFalse(user.containsKey("password"), "no password field");
			Assert.assertFalse(user.containsKey("role"), "no role field");
		}
		Assert.assertTrue(response.time() < 1000, "Response time < 1000ms, was " + response.time() + "ms");
	}

	@Test
	@Description("Create returns 200, not 201")
	public void createUserEchoesPayload()
	{
		String name = tcName("live-user");
		String email = tcEmail();
		Response response = post(ECOMMERCE_USERS_ENDPOINT, json("name", name, "email", email));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.jsonPath().getString("name").startsWith("tc-live-user-"), "tc- tagged name");
		Assert.assertTrue(response.jsonPath().getString("email").endsWith("@example.test"), "email echoed");
		Assert.assertFalse(response.jsonPath().getMap("$").containsKey("password"), "no password echoed");
	}

	@Test(dependsOnMethods = "createUserEchoesPayload")
	public void listUsersContainsCreatedUserWithUniqueIds()
	{
		String id = ensureUserId();
		Response response = get(ECOMMERCE_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		List<String> ids = response.jsonPath().getList("id", String.class);
		Assert.assertEquals(new HashSet<>(ids).size(), ids.size(), "all user ids are unique");
		Assert.assertTrue(ids.contains(id), "created user present in list");
	}

	@Test
	@Description("Prompt assumed GET /users/{id}; only the collection route exists")
	public void getUserByIdReturns404()
	{
		Response response = get(ECOMMERCE_USERS_ENDPOINT + "/" + ensureUserId());
		assertNotFound(response);
	}

	@Test
	public void putUserReturns405()
	{
		Response response = put(ECOMMERCE_USERS_ENDPOINT + "/" + ensureUserId(),
				json("name", "x", "email", "x@example.com"), null);
		assertMethodNotAllowed(response);
	}

	@Test
	@Description("No phone/address/role fields exist on this API at all")
	public void patchUserReturns405()
	{
		Response response = patch(ECOMMERCE_USERS_ENDPOINT + "/" + ensureUserId(),
				json("phone", "+10000000000"), null);
		assertMethodNotAllowed(response);
	}

	@Test
	public void deleteUserReturns405()
	{
		Response response = delete(ECOMMERCE_USERS_ENDPOINT + "/" + ensureUserId(), null);
		assertMethodNotAllowed(response);
	}

	@Test
	@Description("Documents that the email-format rule is not enforced by this API")
	public void createUserWithInvalidEmailIsAccepted()
	{
		Response response = post(ECOMMERCE_USERS_ENDPOINT,
				json("name", tcName("invalid-email"), "email", "not-an-email"));
		Assert.assertEquals(response.statusCode(), 200, "API accepts an invalid email");
		Assert.assertEquals(response.jsonPath().getString("email"), "not-an-email",
				"FINDING: POST /users performs no email format validation");
	}
}
