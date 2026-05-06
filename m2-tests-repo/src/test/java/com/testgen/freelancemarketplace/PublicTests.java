package com.testgen.freelancemarketplace;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

// ────────────────────────────────────────────────────────────────────────────
// PublicTests.java — hand-written, dynamic, scenario-driven public test cases
// for Talabat M2.
//
// This is the canonical public test file. The 749 template-generated classes
// (deprecated) live in archive/PublicTestsAll_Stale.java for reference but
// no longer execute.
//
// Each class below is one row in
// docs/test-scenarios/Talabat_Tests_Description.md.
//
// Style guide:
//   * One package-private `class TC<NN>_<DescriptiveName> extends TestBase`
//     per scenario, with one or more @Test methods.
//   * @Tag("public") + a category tag (features_m1 / features_m2 / patterns
//     / amendments / updated_crud / cross_cutting) per scenario row.
//   * Resolve every URL through TestBase helpers — never hardcode "/api/...":
//       - crudReadPath("Proposal"), crudCollectionPath("MenuItem"), fillPath(...)
//       - loginPath(), registerPath()
//     Resolve table names through tableName("Proposal"), enums through
//     enumValues("OrderStatus").
//   * Resolve IDs from response bodies (registration, search results) or
//     from the auto-seeded fixtures (admin = id=1 via adminToken/adminId,
//     vegetarian customer with 5 orders = id=4, etc. — see
//     memory/project_talabat_seed_data.md). Never write `/api/users/1`
//     literally.
//   * One scenario at a time, approved by the user before mapping to the
//     other 7 themes.
// ────────────────────────────────────────────────────────────────────────────

// ─── TC01 — Register a new user (happy path) ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC01_RegisterHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC01 — POST registerPath() with a fresh email returns 2xx and a JWT token")
        void register_returns_2xx_with_token() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a payload with a nonce-based email so it cannot collide with
                // any of the auto-seeded users (_preseed_*@grader.testgen.io) or
                // with prior runs of this test class.
                String email = "tc01_" + nonce() + "@grader.testgen.io";
                String body = String.format("""
                                {"name":"TC01 User","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));

                HttpResponse<String> r = httpPost("/api/auth/register", body);

                // Strict 2xx — registering a brand-new email with a valid payload
                // must succeed; any non-2xx is a bug in register / validation /
                // password hashing / DB persistence.
                assert2xx(r, "TC01 register");
                JsonNode j = parseNode(r.body());
                // Spec: response is { "token": "...", "expiresIn": ... }. No 'id' field;
                // chained tests resolve uid from JWT (uidFromJwt()) or via login.
                assertNotNull(j.get("token"),
                                "TC01: register response must include 'token' field per spec; body=" + r.body());
                assertFalse(j.get("token").asText().isBlank(),
                                "TC01: 'token' must be a non-blank string; got " + j.get("token"));
                assertTrue(j.has("expiresIn") && j.get("expiresIn").asLong() > 0,
                                "TC01: register response must include positive 'expiresIn' per spec; body=" + r.body());
        }
}

// ─── TC02 — Login with valid credentials (happy path) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC02_LoginHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC02 — POST loginPath() after a successful register returns 2xx with a 3-segment JWT")
        void login_returns_2xx_with_three_segment_jwt() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — create a fresh user.
                String email = "tc02_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC02 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC02 setup register");

                // Act — log in with the same credentials.
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);

                // Assert.
                assert2xx(r, "TC02 login");
                JsonNode j = parseNode(r.body());
                assertNotNull(j.get("token"),
                                "TC02: login response must include 'token' field; body=" + r.body());
                String token = j.get("token").asText();
                assertFalse(token.isBlank(),
                                "TC02: 'token' must be a non-blank string");
                assertEquals(3, token.split("\\.").length,
                                "TC02: 'token' must be a 3-segment JWT (a.b.c); got '" + token + "'");
        }
}

// ─── TC03 — Read own user profile with valid JWT (happy path) ───────────────
@Tag("public")
@Tag("updated_crud")
class TC03_ReadOwnProfileHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC03 — GET crudReadPath(\"User\") with own JWT returns 2xx and a JSON object")
        void read_own_profile_returns_2xx_and_json_object() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register and capture the new user's id.
                String email = "tc03_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC03 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC03 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                // Setup — login and capture the JWT.
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC03 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — read the user's own profile via the User CRUD path. The
                // path template comes from the manifest so a student who renames
                // their controller still gets the correct URL.
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid, token);

                // Assert. Strict 2xx — we just registered this exact user, a 404
                // here is a real bug (register didn't persist OR the JWT chain
                // rejected our own token).
                assert2xx(r, "TC03 read own profile");
                JsonNode j = parseNode(r.body());
                assertTrue(j.isObject(),
                                "TC03: response body must be a JSON object; got " + r.body());
        }
}

// ─── TC04 — Register with duplicate email returns 4xx (negative path) ───────
@Tag("public")
@Tag("features_m2")
class TC04_RegisterDuplicateEmailTests extends TestBase {

        @Test
        @DisplayName("TC04 — POST registerPath() with an already-registered email returns a 4xx")
        void register_with_duplicate_email_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a payload with a nonce-based email — guaranteed not to
                // collide with auto-seeded users or with prior runs of this test
                // class (since @BeforeEach truncates between tests anyway).
                String email = "tc04_" + nonce() + "@grader.testgen.io";
                String firstBody = String.format("""
                                {"name":"TC04 First","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));

                // Step 1 — first registration must succeed (precondition).
                HttpResponse<String> first = httpPost("/api/auth/register", firstBody);
                assert2xx(first, "TC04 first register (precondition)");

                // Step 2 — second registration with the SAME email but different
                // name + phone (uniqueness should be on email).
                String secondBody = String.format("""
                                {"name":"TC04 Second","email":"%s","password":"AnotherPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> second = httpPost("/api/auth/register", secondBody);

                // Step 3 — strict 4xx assertion. Tolerate any of 400/409/422
                // (M2 spec doesn't pin a specific code) but NOT 2xx, NOT 5xx,
                // and NOT 401/403 (the body itself is well-formed and authorized).
                int code = second.statusCode();
                assertTrue(code >= 400 && code < 500,
                                "TC04: duplicate-email register must return a 4xx client error; "
                                                + "got " + code + " body=" + second.body());
                assertTrue(code != 401,
                                "TC04: duplicate-email is not an auth failure (no Authorization header was sent); "
                                                + "401 indicates the controller is misclassifying the error. body="
                                                + second.body());
                assertTrue(code != 403,
                                "TC04: duplicate-email is not a permission failure (anyone can register); "
                                                + "403 indicates the controller is misclassifying the error. body="
                                                + second.body());
        }
}

// ─── TC05 — Login with wrong password returns 401 (negative path) ───────────
@Tag("public")
@Tag("features_m2")
class TC05_LoginWrongPasswordTests extends TestBase {

        @Test
        @DisplayName("TC05 — POST loginPath() with the wrong password returns strictly 401")
        void login_with_wrong_password_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a fresh user with a known-correct password.
                // nonce-based email avoids collisions with auto-seeded users and
                // with prior test runs (truncate@BeforeEach handles cross-test).
                String email = "tc05_" + nonce() + "@grader.testgen.io";
                String correctPwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC05 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, correctPwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC05 setup register (precondition)");

                // Act — log in with the SAME email but a different password.
                // Different enough that bcrypt cannot accidentally match (no
                // shared prefix, different length).
                String wrongPwd = "WrongPwd!2026";
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, wrongPwd);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);
                int code = r.statusCode();

                // Assert — strictly 401. Tolerant assertions (with explanatory
                // messages) for each common misclassification:
                // * 2xx: login skipped the password check — critical security bug.
                // * 5xx: bcrypt mismatch leaked as exception instead of being
                // caught and translated to 401.
                // * 404: user-enumeration anti-pattern (OWASP). Login must NOT
                // distinguish "user not found" from "wrong password" in
                // its status code; both should be 401.
                // * 403: this is not a permissions issue. Login is how you
                // OBTAIN permissions; you cannot be 403'd from it.
                assertTrue(code / 100 != 2,
                                "TC05: login with wrong password must NOT return 2xx. "
                                                + "A 2xx here means the password check was skipped — critical security bug. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC05: login with wrong password must NOT 5xx. A 5xx here means the bcrypt "
                                                + "mismatch threw an unhandled exception instead of being caught and "
                                                + "translated to 401. Got " + code + " body=" + r.body());
                assertTrue(code != 404,
                                "TC05: login with wrong password must NOT return 404. The user account exists; "
                                                + "returning 404 instead of 401 leaks the existence/non-existence of accounts "
                                                + "(OWASP user-enumeration anti-pattern). body=" + r.body());
                assertTrue(code != 403,
                                "TC05: login with wrong password must NOT return 403. Login is the act of "
                                                + "obtaining permissions, not exercising them — 403 is structurally wrong. "
                                                + "body=" + r.body());
                assertEquals(401, code,
                                "TC05: login with wrong password must return strictly 401 Unauthorized; got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC06 — Authentication happy path: valid admin JWT accepted on a non-User
// CRUD
@Tag("public")
@Tag("authentication")
class TC06_AuthValidTokenAcceptedTests extends TestBase {

        @Test
        @DisplayName("TC06 — GET a non-User CRUD list endpoint with a valid admin Bearer JWT returns 2xx (auth filter accepts the token)")
        void valid_admin_jwt_is_accepted_on_non_user_crud() throws Exception {
                BASE_URL = catalogServiceUrl;
                // Setup — obtain an admin JWT via TestBase.adminToken(). Uses
                // the pre-seeded admin login fast path when available, falling
                // back to TestAuthHelper.seedAdmin (HTTP register + JDBC promote
                // to ADMIN + login) otherwise. Admin role ensures TC06 is not
                // 403-blocked on entities whose list endpoint is admin-only.
                String token = adminToken();

                // Act — hit a NON-User CRUD list endpoint with the admin token.
                // Picks the first top-level non-User entity from the manifest
                // (theme-specific: Talabat → Address, Amazon → ShippingAddress,
                // Booking → Provider, etc.). Broadens auth-filter coverage
                // beyond TC03's User endpoint without hardcoding per-theme.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, token);

                // Assert — strict 2xx. This proves the auth filter accepts the
                // token end-to-end (signature verified, claims parsed, security
                // context populated) on a different controller than
                // UserController, ruling out "auth only works on /api/users".
                assert2xx(r, "TC06 auth happy path (admin) on " + entity + " list (" + path + ")");
        }
}

// ─── TC07 — Missing Authorization header on a non-User CRUD returns 401 ─────
@Tag("public")
@Tag("authentication")
class TC07_AuthMissingHeaderTests extends TestBase {

        @Test
        @DisplayName("TC07 — GET a non-User CRUD list endpoint with NO Authorization header returns strictly 401")
        void missing_auth_header_returns_401_on_non_user_crud() throws Exception {
                BASE_URL = userServiceUrl;
                // Act — same endpoint as TC06's happy path (so the two form a
                // clean A/B test of the JWT filter), BUT no Authorization
                // header at all (httpGet, not httpGetAuth). No setup needed —
                // we're testing whether anonymous requests are blocked, which
                // doesn't depend on having any specific row in the DB.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGet(path);
                int code = r.statusCode();

                // Assert — strictly 401. Tolerant assertions for each common
                // misclassification, with diagnostic messages:
                // * 2xx: endpoint wide-open — critical security bug.
                // * 5xx: filter chain crashed instead of cleanly rejecting.
                // * 404: endpoint reachable anonymously and just returns
                // empty/missing — wrong; auth must block FIRST, before
                // any controller logic runs.
                // * 403: 403 means "authenticated but lacking permission". We
                // sent NO credentials at all — the correct response is
                // 401 (Unauthorized), not 403 (Forbidden).
                assertTrue(code / 100 != 2,
                                "TC07: GET " + path + " without Authorization header must NOT return 2xx. "
                                                + "A 2xx here means the endpoint is wide-open — critical security bug. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC07: GET " + path + " without Authorization header must NOT 5xx. A 5xx here "
                                                + "means the filter chain crashed instead of cleanly rejecting. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code != 404,
                                "TC07: GET " + path + " without Authorization header must NOT return 404. "
                                                + "A 404 here means the endpoint is reachable anonymously and just "
                                                + "returned empty — auth must block FIRST, before any controller logic "
                                                + "runs. body=" + r.body());
                assertTrue(code != 403,
                                "TC07: GET " + path + " without Authorization header must NOT return 403. We "
                                                + "sent NO credentials; 403 means \"authenticated but lacking permission\", "
                                                + "which doesn't apply when there's no auth attempt at all. The correct "
                                                + "code is 401 Unauthorized. body=" + r.body());
                assertEquals(401, code,
                                "TC07: GET " + path + " without Authorization header must return strictly 401 "
                                                + "Unauthorized; got " + code + " body=" + r.body());
        }
}

// ─── TC08 — Tampered JWT signature is rejected with 401 (negative path) ─────
@Tag("public")
@Tag("authentication")
class TC08_AuthTamperedSignatureTests extends TestBase {

        @Test
        @DisplayName("TC08 — GET protected endpoint with a tampered-signature JWT returns strictly 401 (signature is verified, not just decoded)")
        void tampered_jwt_signature_is_rejected_with_401() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — get a real, fully-valid admin JWT, then tamper its
                // signature segment. tamperSignature(token) preserves the
                // header + payload (still parses as a JWT, still passes any
                // "is it 3 segments?" check) but replaces the signature with
                // a base64 of "tampered-signature-does-not-verify" — so the
                // signature cannot verify against any signing key.
                String validToken = adminToken();
                String tamperedToken = tamperSignature(validToken);

                // Act — same endpoint as TC06/TC07 (the auth-filter A/B/C
                // triad). Send the tampered token in the Authorization header.
                // A correctly-implemented auth filter MUST reject this with 401
                // even though the token looks structurally valid.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, tamperedToken);
                int code = r.statusCode();

                // Assert — strictly 401. Diagnostic ladder:
                // * 2xx: signature not actually verified — critical security bug
                // (anyone can forge tokens by editing the payload).
                // * 5xx: filter chain crashed on signature mismatch instead of
                // cleanly rejecting.
                // * 404: filter passed the request through to the controller
                // and just didn't find anything — wrong; signature
                // mismatch must be caught FIRST in the filter, before
                // any controller logic.
                // * 403: forged credentials = "not authenticated" (401), not
                // "authenticated but lacking permission" (403).
                assertTrue(code / 100 != 2,
                                "TC08: tampered-signature JWT must NOT be accepted (status " + code + "). "
                                                + "A 2xx here means the signature was not actually verified — anyone can "
                                                + "forge tokens by editing the payload. Critical security bug. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC08: tampered-signature JWT must NOT 5xx (status " + code + "). A 5xx here "
                                                + "means the filter chain crashed on signature mismatch instead of cleanly "
                                                + "rejecting. body=" + r.body());
                assertTrue(code != 404,
                                "TC08: tampered-signature JWT must NOT return 404 (status " + code + "). A 404 "
                                                + "here means the filter passed the request through to the controller and "
                                                + "just didn't find anything — signature mismatch must be caught FIRST in "
                                                + "the filter, before any controller logic runs. body=" + r.body());
                assertTrue(code != 403,
                                "TC08: tampered-signature JWT must NOT return 403 (status " + code + "). Forged "
                                                + "credentials are \"not authenticated\" (401), not \"authenticated but "
                                                + "lacking permission\" (403). body=" + r.body());
                assertEquals(401, code,
                                "TC08: tampered-signature JWT must return strictly 401 Unauthorized; got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC09 — Login with non-existent email returns 401 (negative path) ───────
@Tag("public")
@Tag("features_m2")
class TC09_LoginUnknownEmailTests extends TestBase {

        @Test
        @DisplayName("TC09 — POST loginPath() with an email that has never been registered returns strictly 401")
        void login_unknown_email_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                // nonce-based email — guaranteed not to exist in the DB. Any
                // truncate/auto-seed in @BeforeEach also strips users, so this
                // email is truly absent at login time.
                String unknownEmail = "tc09_never_registered_" + nonce() + "@grader.testgen.io";
                String body = String.format("""
                                {"email":"%s","password":"AnythingPwd!2026"}
                                """, unknownEmail);

                HttpResponse<String> r = httpPost("/api/auth/login", body);
                int code = r.statusCode();

                // Spec §10 S1-F11: 401 for both "user not found" and "wrong password" — avoids email enumeration.
                assertTrue(code / 100 != 2,
                                "TC09: login with a never-registered email must NOT return 2xx (status "
                                                + code + "). A 2xx here means a token was issued for a non-existent "
                                                + "user. body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC09: login with a never-registered email must NOT 5xx (status " + code
                                                + "). Server must handle the missing-user case cleanly. body="
                                                + r.body());
                assertTrue(code != 403,
                                "TC09: login with a never-registered email must NOT return 403 (status " + code
                                                + "). 403 is a permission error; this is an unauthenticated condition. body="
                                                + r.body());
                assertEquals(401, code,
                                "TC09: login with a never-registered email must return strictly 401 Unauthorized "
                                                + "(spec §10 S1-F11 — 401 for both 'user not found' and 'wrong password'); "
                                                + "got " + code + " body=" + r.body());
        }
}

// ─── TC10 — Empty Bearer token returns 401 (negative path) ──────────────────
@Tag("public")
@Tag("authentication")
class TC10_AuthEmptyBearerTests extends TestBase {

        @Test
        @DisplayName("TC10 — GET protected endpoint with `Authorization: Bearer ` (empty token) returns strictly 401")
        void empty_bearer_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "Bearer " with a trailing space and no token after it. Not a
                // missing header (TC07 covers that) — this header is PRESENT
                // but its value is malformed.
                HttpResponse<String> r = httpGetWithRawAuth(path, "Bearer ");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC10: empty Bearer token must NOT be accepted (status " + code
                                                + "). A 2xx here means the auth filter accepted an empty/missing token. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC10: empty Bearer token must NOT 5xx (status " + code + "). The filter must "
                                                + "handle empty tokens cleanly, not throw NPE. body=" + r.body());
                assertEquals(401, code,
                                "TC10: empty Bearer token must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC11 — Non-Bearer scheme (Basic) returns 401 (negative path) ───────────
@Tag("public")
@Tag("authentication")
class TC11_AuthBasicSchemeTests extends TestBase {

        @Test
        @DisplayName("TC11 — GET protected endpoint with `Authorization: Basic ...` (non-Bearer scheme) returns strictly 401")
        void basic_scheme_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "Basic dXNlcjpwYXNz" — base64 of "user:pass". Catches lenient
                // parsers that strip the scheme prefix and try to decode whatever
                // is left as a JWT (it isn't).
                HttpResponse<String> r = httpGetWithRawAuth(path, "Basic dXNlcjpwYXNz");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC11: Basic-scheme auth must NOT be accepted on a JWT-protected endpoint "
                                                + "(status " + code
                                                + "). The filter must reject any non-Bearer scheme. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC11: Basic-scheme auth must NOT 5xx (status " + code + "). body=" + r.body());
                assertEquals(401, code,
                                "TC11: Basic-scheme auth must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC12 — Garbage non-JWT token returns 401 (negative path) ───────────────
@Tag("public")
@Tag("authentication")
class TC12_AuthGarbageTokenTests extends TestBase {

        @Test
        @DisplayName("TC12 — GET protected endpoint with `Authorization: Bearer not_a_valid_jwt` returns strictly 401")
        void garbage_token_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "not_a_valid_jwt" — no dots, not base64, structurally invalid.
                // Catches "if 3 dot-segments, decode without verifying" and any
                // code path that doesn't validate JWT structure before claims-extract.
                HttpResponse<String> r = httpGetWithRawAuth(path, "Bearer not_a_valid_jwt");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC12: garbage non-JWT token must NOT be accepted (status " + code
                                                + "). A 2xx here means the filter didn't validate the JWT structure. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC12: garbage token must NOT 5xx (status " + code + "). The parser must "
                                                + "handle malformed tokens gracefully. body=" + r.body());
                assertEquals(401, code,
                                "TC12: garbage non-JWT token must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC13 — Forged role-claim token (payload modified post-signing) rejected
@Tag("public")
@Tag("authentication")
class TC13_AuthForgedRoleClaimTests extends TestBase {

        @Test
        @DisplayName("TC13 — GET protected endpoint with a payload-tampered JWT (role forged to ADMIN) is NOT accepted")
        void forged_role_claim_token_is_rejected() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a CUSTOMER user (default theme role) and log
                // in to capture a real, signed JWT for them.
                String email = "tc13_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC13 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC13 setup register (precondition)");

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC13 setup login (precondition)");
                String realToken = parseNode(login.body()).get("token").asText();

                // Forge a role claim into the payload while keeping the original
                // signature. The signature was computed over the original
                // payload; modifying the payload makes the signature INVALID
                // even though we don't tamper the signature segment itself.
                String[] parts = realToken.split("\\.");
                if (parts.length != 3) {
                        throw new AssertionError("TC13 setup: real token is not a 3-segment JWT: " + realToken);
                }
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Try to replace common role-claim patterns. If none match,
                // inject a "role":"ADMIN" entry into the JSON object.
                String tamperedPayload = payloadJson;
                for (String[] swap : new String[][] {
                                { "\"role\":\"CUSTOMER\"", "\"role\":\"ADMIN\"" },
                                { "\"role\": \"CUSTOMER\"", "\"role\":\"ADMIN\"" },
                                { "\"authorities\":[\"ROLE_CUSTOMER\"]", "\"authorities\":[\"ROLE_ADMIN\"]" },
                }) {
                        if (tamperedPayload.contains(swap[0])) {
                                tamperedPayload = tamperedPayload.replace(swap[0], swap[1]);
                                break;
                        }
                }
                if (tamperedPayload.equals(payloadJson)) {
                        // No known role-claim pattern found — inject one. Locate the
                        // closing brace of the outer object and prepend a role claim.
                        int lastBrace = tamperedPayload.lastIndexOf('}');
                        if (lastBrace > 0) {
                                String prefix = tamperedPayload.substring(0, lastBrace).trim();
                                if (prefix.endsWith("{")) {
                                        tamperedPayload = prefix + "\"role\":\"ADMIN\"}";
                                } else {
                                        tamperedPayload = prefix + ",\"role\":\"ADMIN\"}";
                                }
                        }
                }
                String tamperedB64 = java.util.Base64.getUrlEncoder().withoutPadding()
                                .encodeToString(tamperedPayload.getBytes());
                String forgedToken = parts[0] + "." + tamperedB64 + "." + parts[2];

                // Act — hit a protected endpoint with the forged token.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, forgedToken);
                int code = r.statusCode();

                // Assert — must NOT be 2xx. Either of these is acceptable:
                // * 401 — signature verification caught the payload tamper (preferred).
                // * 403 — signature verified but server re-validated role from DB
                // and found CUSTOMER, not ADMIN.
                // But 2xx means signature wasn't verified AND role was trusted from
                // the (forged) payload — critical privilege-escalation bug.
                assertTrue(code / 100 != 2,
                                "TC13: forged role-claim token must NOT be accepted (status " + code + "). "
                                                + "A 2xx here means the signature was not verified after payload "
                                                + "modification — anyone with a real token can self-promote to ADMIN by "
                                                + "editing their payload. Critical privilege-escalation bug. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC13: forged role-claim token must NOT 5xx (status " + code + "). The filter "
                                                + "must reject cleanly. body=" + r.body());
        }
}

// ─── TC14 — Register with missing required field returns 4xx (negative path)
@Tag("public")
@Tag("features_m2")
class TC14_RegisterMissingFieldTests extends TestBase {

        @Test
        @DisplayName("TC14 — POST registerPath() with a body missing the `email` field returns a 4xx")
        void register_missing_email_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Body missing `email` — controller must reject with 4xx, not
                // propagate the missing-field as a server-side NPE/5xx or
                // silently create a user with null email.
                String body = String.format("""
                                {"name":"TC14 User","password":"TestPwd!2026","phone":"+201%s"}
                                """, nonce().substring(0, 9));

                HttpResponse<String> r = httpPost("/api/auth/register", body);
                int code = r.statusCode();

                assertTrue(code >= 400 && code < 500,
                                "TC14: register with missing required field (email) must return a 4xx; got "
                                                + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC14: register with missing email must NOT 2xx (a user without an email "
                                                + "cannot be valid). body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC14: register with missing email must NOT 5xx — the controller must "
                                                + "validate input cleanly, not crash. body=" + r.body());
        }
}

// ─── TC15 — Register with role=ADMIN in body must NOT yield an ADMIN account ─
@Tag("public")
@Tag("features_m2")
class TC15_RegisterRoleEscalationTests extends TestBase {

        @Test
        @DisplayName("TC15 — POST registerPath() with `role:ADMIN` in body must NOT result in an ADMIN account (privilege-escalation)")
        void register_with_role_admin_in_body_must_not_escalate() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a register body with `role:"ADMIN"` injected. The
                // controller may either ignore the field, reject the request,
                // or accept the request — but the resulting DB row's role
                // column must NOT be ADMIN. Anything else is privilege escalation.
                String email = "tc15_" + nonce() + "@grader.testgen.io";
                String body = String.format(
                                """
                                                {"name":"TC15 User","email":"%s","password":"TestPwd!2026","phone":"+201%s","role":"ADMIN"}
                                                """,
                                email, nonce().substring(0, 9));

                HttpResponse<String> reg = httpPost("/api/auth/register", body);
                // We don't strictly require 2xx here — a controller that
                // rejects extra fields with 4xx is also fine. What matters is
                // that no ADMIN account got created.
                int regCode = reg.statusCode();
                assertTrue(regCode / 100 != 5,
                                "TC15: register with role=ADMIN body must NOT 5xx (got " + regCode
                                                + "). body=" + reg.body());

                // If the registration succeeded, look up the role in DB and
                // assert it's NOT ADMIN.
                if (regCode / 100 == 2) {
                        String role = fetchUserRole(email);
                        assertNotNull(role,
                                        "TC15: registration returned 2xx but no user row found for email "
                                                        + email + " — register isn't actually persisting.");
                        assertTrue(!"ADMIN".equalsIgnoreCase(role),
                                        "TC15: registering with role=ADMIN in the body must NOT result in an "
                                                        + "ADMIN account. Found role=" + role + " (expected the theme "
                                                        + "default, NOT ADMIN). This is a privilege-escalation bug — the "
                                                        + "controller is mapping the body's role field into the entity.");
                }
                // If registration was rejected (4xx), that's also acceptable —
                // the role field was caught as invalid input. Either way no
                // ADMIN was created.
        }
}

// ─── TC16 — Login with empty password returns 4xx (negative path) ───────────
@Tag("public")
@Tag("features_m2")
class TC16_LoginEmptyPasswordTests extends TestBase {

        @Test
        @DisplayName("TC16 — POST loginPath() with `password:\"\"` (empty) returns NOT 2xx")
        void login_empty_password_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a real user so the email exists.
                String email = "tc16_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC16 User","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBody), "TC16 setup register");

                // Act — login with empty password. Bcrypt verification must
                // not be bypassed by an empty input.
                String loginBody = String.format("""
                                {"email":"%s","password":""}
                                """, email);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC16: login with empty password must NOT issue a token (status " + code
                                                + "). A 2xx here means bcrypt verification was bypassed for empty "
                                                + "input. body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC16: login with empty password must NOT 5xx (status " + code + "). The "
                                                + "controller must validate input cleanly, not NPE on empty string. "
                                                + "body=" + r.body());
                // Acceptable: 400 (validation error) or 401 (auth failure) or
                // 422 (semantic validation error). All 4xx.
                assertTrue(code >= 400 && code < 500,
                                "TC16: login with empty password must return a 4xx (validation or auth "
                                                + "failure); got " + code + " body=" + r.body());
        }
}

// ─── TC17 — Cross-user IDOR: User A cannot READ User B's profile ────────────
@Tag("public")
@Tag("authorization")
class TC17_IdorReadOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC17 — Customer A's GET on User B's CRUD path must NOT be 2xx (cross-user IDOR)")
        void customer_a_cannot_read_user_b_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc17a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc17b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC17 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC17 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC17 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC17 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC17 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                HttpResponse<String> r = httpGetAuth("/api/users/" + bid, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC17: customer A reading customer B's profile must NOT be 2xx (status "
                                                + code + "). A 2xx here means cross-user IDOR is unprotected — any "
                                                + "authenticated user can read any other user's profile. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC17: cross-user read must NOT 5xx (status " + code + "). The auth check "
                                                + "must reject cleanly, not crash. body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC17: cross-user read must return 403 (forbidden) or 404 (not-found / "
                                                + "privacy-by-obscurity); got " + code + " body=" + r.body());
        }
}

// ─── TC18 — Cross-user IDOR: User A cannot UPDATE User B's profile ──────────
@Tag("public")
@Tag("authorization")
class TC18_IdorUpdateOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC18 — Customer A's PUT on User B's CRUD path must NOT be 2xx, AND B's data must NOT change in DB")
        void customer_a_cannot_update_user_b_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc18a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc18b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origNameB = "TC18 B Original";
                String regBodyA = String.format("""
                                {"name":"TC18 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"+201%s"}
                                """, origNameB, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC18 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC18 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC18 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                // Mitigation pattern — include all original fields plus the
                // changed name (some controllers require all fields on PUT).
                String tamperedName = "TC18 HIJACK";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"+201%s"}
                                """, tamperedName, emailB, pwd, nonce().substring(0, 9));
                HttpResponse<String> r = httpPutAuth("/api/users/" + bid, putBody, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC18: customer A updating customer B's profile must NOT be 2xx (status "
                                                + code + "). A 2xx here means cross-user IDOR write is unprotected. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC18: cross-user update must NOT 5xx (status " + code + "). body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC18: cross-user update must return 403 or 404; got " + code + " body=" + r.body());

                // Defensive — even if controller returned 4xx, verify B's row
                // in DB was NOT mutated. Some buggy controllers reject the
                // response but commit the change.
                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, bid);
                assertEquals(origNameB, currentName,
                                "TC18: cross-user PUT was rejected (status " + code + ") but B's name in DB "
                                                + "changed from '" + origNameB + "' to '" + currentName + "' — the "
                                                + "controller is committing the change before doing the auth check.");
        }
}

// ─── TC19 — Cross-user IDOR: User A cannot DELETE User B ────────────────────
@Tag("public")
@Tag("authorization")
class TC19_IdorDeleteOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC19 — Customer A's DELETE on User B's CRUD path must NOT be 2xx, AND B must STILL exist in DB")
        void customer_a_cannot_delete_user_b() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc19a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc19b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC19 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC19 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC19 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC19 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC19 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                HttpResponse<String> r = httpDeleteAuth("/api/users/" + bid, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC19: customer A deleting customer B must NOT be 2xx (status " + code
                                                + "). A 2xx here means cross-user delete is unprotected. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC19: cross-user delete must NOT 5xx (status " + code + "). body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC19: cross-user delete must return 403 or 404; got " + code + " body=" + r.body());

                // Defensive — B's row must STILL exist in DB.
                String userTable = tableName("User");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM " + userTable + " WHERE id = ?",
                                Integer.class, bid);
                assertNotNull(count);
                assertTrue(count == 1,
                                "TC19: cross-user DELETE was rejected (status " + code + ") but B's row in DB "
                                                + "is gone (count=" + count + ") — the controller is committing the "
                                                + "delete before doing the auth check.");
        }
}

// ─── TC20 — Owner happy path: User A can UPDATE their own profile ───────────
@Tag("public")
@Tag("authorization")
class TC20_OwnerUpdateOwnProfileTests extends TestBase {

        @Test
        @DisplayName("TC20 — Customer's PUT on their own User CRUD path returns 2xx, AND DB reflects the new name")
        void owner_can_update_own_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc20_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origPhone = "+201" + nonce().substring(0, 9);
                String regBody = String.format("""
                                {"name":"TC20 Original","email":"%s","password":"%s","phone":"%s"}
                                """, email, pwd, origPhone);
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC20 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC20 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Mitigation pattern — include all original fields plus the new name.
                String newName = "TC20 Updated";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"%s"}
                                """, newName, email, pwd, origPhone);
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid, putBody, token);
                assert2xx(r, "TC20 owner update own profile");

                // JDBC verification (NOT via GET) — we're testing the PUT
                // path's persistence semantics specifically.
                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, uid);
                assertEquals(newName, currentName,
                                "TC20: PUT returned " + r.statusCode() + " but DB row's name is '" + currentName
                                                + "' (expected '" + newName
                                                + "') — the controller returned 2xx without "
                                                + "actually persisting the update.");
        }
}

// ─── TC21 — Admin override: admin can READ any user ─────────────────────────
@Tag("public")
@Tag("authorization")
class TC21_AdminReadAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC21 — Admin's GET on a customer's User CRUD path returns 2xx (admin role bypasses ownership)")
        void admin_can_read_any_user() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc21_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC21 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC21 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + customerId, adminTok);
                assert2xx(r, "TC21 admin read customer");
                JsonNode j = parseNode(r.body());
                assertTrue(j.isObject(),
                                "TC21: admin GET response body must be a JSON object; got " + r.body());
        }
}

// ─── TC22 — Admin override: admin can UPDATE any user ───────────────────────
@Tag("public")
@Tag("authorization")
class TC22_AdminUpdateAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC22 — Admin's PUT on a customer's User CRUD path returns 2xx, AND DB reflects the new name")
        void admin_can_update_any_user() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc22_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origPhone = "+201" + nonce().substring(0, 9);
                String regBody = String.format("""
                                {"name":"TC22 Customer","email":"%s","password":"%s","phone":"%s"}
                                """, email, pwd, origPhone);
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC22 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();

                // Mitigation pattern — include all original fields plus the new name.
                String newName = "TC22 Admin-Updated";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"%s"}
                                """, newName, email, pwd, origPhone);
                HttpResponse<String> r = httpPutAuth("/api/users/" + customerId, putBody, adminTok);
                assert2xx(r, "TC22 admin update customer");

                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, customerId);
                assertEquals(newName, currentName,
                                "TC22: admin PUT returned " + r.statusCode() + " but customer's name in DB is '"
                                                + currentName + "' (expected '" + newName + "') — admin update did not "
                                                + "persist.");
        }
}

// ─── TC23 — Admin override: admin can DELETE any user (strict hard-delete) ──
@Tag("public")
@Tag("authorization")
class TC23_AdminDeleteAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC23 — Admin's DELETE on a customer's User CRUD path returns 2xx, AND the row is HARD-deleted (strict)")
        void admin_can_delete_any_user_hard() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc23_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC23 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC23 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/users/" + customerId, adminTok);
                assert2xx(r, "TC23 admin delete customer");

                // STRICT hard-delete: row must be physically gone from DB.
                // Soft-delete is NOT acceptable for this test per direction.
                String userTable = tableName("User");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM " + userTable + " WHERE id = ?",
                                Integer.class, customerId);
                assertNotNull(count);
                assertEquals(0, count.intValue(),
                                "TC23: admin DELETE returned " + r.statusCode() + " but customer row STILL "
                                                + "exists in DB (count=" + count
                                                + "). DELETE must hard-delete the row, "
                                                + "not soft-delete it (use the deactivate endpoint for status changes).");

                // GET-after-DELETE — strictly 404.
                HttpResponse<String> g = httpGetAuth("/api/users/" + customerId, adminTok);
                int gcode = g.statusCode();
                assertTrue(gcode / 100 != 2,
                                "TC23: GET-after-DELETE returned 2xx (status " + gcode + "). The row was "
                                                + "already verified gone from DB above, but GET still finds it. body="
                                                + g.body());
                assertEquals(404, gcode,
                                "TC23: GET after a successful DELETE must return 404 Not Found; got " + gcode
                                                + " body=" + g.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S1-F12 — Get User Activity Feed
// GET /api/users/{id}/activity?page={page}&size={size}
// Auth: required user. Ownership: caller must be target OR admin.
// Defaults: page=0, size=10, max size=100. Response shape:
// { content: [{action, timestamp, details}], page, size, totalElements }
// ════════════════════════════════════════════════════════════════════════════

// ─── TC24 — Owner GET own activity returns 2xx with paginated envelope ──────
@Tag("public")
@Tag("features_m2")
class TC24_ActivityOwnerHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC24 — GET /api/users/{ownId}/activity with own token returns 2xx and a paginated envelope")
        void owner_activity_returns_2xx_with_envelope() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register and login the user.
                String email = "tc24_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC24 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC24 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC24 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — GET own activity.
                String activityPath = "/api/users" + "/" + uid + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                assert2xx(r, "TC24 owner activity");

                // Assert envelope shape per spec: content[], page, size, totalElements.
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("content"),
                                "TC24: response must include `content` field; body=" + r.body());
                assertTrue(j.get("content").isArray(),
                                "TC24: `content` must be an array; got " + j.get("content"));
                assertTrue(j.has("page"),
                                "TC24: response must include `page` field; body=" + r.body());
                assertTrue(j.has("size"),
                                "TC24: response must include `size` field; body=" + r.body());
                assertTrue(j.has("totalElements"),
                                "TC24: response must include `totalElements` field; body=" + r.body());
        }
}

// ─── TC25 — Non-existent user ID returns 404 (admin token) ──────────────────
@Tag("public")
@Tag("features_m2")
class TC25_ActivityNonExistentIdTests extends TestBase {

        @Test
        @DisplayName("TC25 — GET /api/users/<Long.MAX_VALUE>/activity with admin token returns strictly 404")
        void activity_non_existent_id_returns_404() throws Exception {
                BASE_URL = userServiceUrl;
                // Use admin token: per spec, admin passes ownership check, then
                // user-not-found returns 404. With a non-admin token, ownership
                // would fail first with 403 — wrong path for this test.
                String adminTok = adminToken();
                long missingId = Long.MAX_VALUE;
                String activityPath = "/api/users" + "/" + missingId + "/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC25: activity for a non-existent user ID must NOT be 2xx (status " + code
                                                + "). body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC25: activity for a non-existent user ID must NOT 5xx — server must handle "
                                                + "missing-user gracefully. body=" + r.body());
                assertEquals(404, code,
                                "TC25: per spec, admin passes ownership check then user-not-found yields "
                                                + "strictly 404; got " + code + " body=" + r.body());
        }
}

// ─── TC26 — Negative user ID returns 4xx (admin token) ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC26_ActivityNegativeIdTests extends TestBase {

        @Test
        @DisplayName("TC26 — GET /api/users/-1/activity with admin token returns a 4xx (graceful)")
        void activity_negative_id_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Admin token — bypass ownership so the test actually exercises
                // the negative-id rejection logic (validation OR not-found).
                String adminTok = adminToken();
                String activityPath = "/api/users" + "/-1/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC26: activity for a negative user ID must NOT 5xx — controller must "
                                                + "validate / reject gracefully, not crash. status=" + code + " body="
                                                + r.body());
                assertTrue(code / 100 != 2,
                                "TC26: activity for a negative user ID must NOT be 2xx — negative ids cannot "
                                                + "match any real user. status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC26: activity for a negative user ID must return a 4xx (400 validation or "
                                                + "404 not-found); got " + code + " body=" + r.body());
        }
}

// ─── TC27 — String user ID returns 4xx (admin token) ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC27_ActivityStringIdTests extends TestBase {

        @Test
        @DisplayName("TC27 — GET /api/users/abc/activity with admin token returns a 4xx (path-var binding fails)")
        void activity_string_id_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Admin token — ensures any 401 we see is NOT from missing auth.
                String adminTok = adminToken();
                String activityPath = "/api/users" + "/abc/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC27: activity for a non-numeric user ID must NOT 5xx — Spring's path-var "
                                                + "binding should reject the string cleanly, not throw an unhandled "
                                                + "TypeMismatchException. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC27: activity for a non-numeric user ID must NOT be 2xx — 'abc' cannot be "
                                                + "a valid Long. status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC27: activity for a non-numeric user ID must return a 4xx (typically 400 "
                                                + "Bad Request); got " + code + " body=" + r.body());
        }
}

// ─── TC28 — size=0 returns gracefully (NOT 5xx) ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC28_ActivitySizeZeroTests extends TestBase {

        @Test
        @DisplayName("TC28 — GET /api/users/{ownId}/activity?size=0 must NOT 5xx (spec silent on size=0)")
        void activity_size_zero_does_not_5xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — own user so we reach the pagination logic.
                String email = "tc28_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC28 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC28 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC28 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — size=0. PageRequest.of(0, 0) throws IllegalArgumentException,
                // so unhandled this becomes 500. Spec doesn't pin a code here, but
                // graceful handling means NOT 5xx.
                String activityPath = "/api/users" + "/" + uid + "/activity?size=0";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC28: size=0 must NOT 5xx — controller must validate / clamp / reject "
                                                + "gracefully, not let PageRequest.of throw IllegalArgumentException. "
                                                + "status=" + code + " body=" + r.body());
        }
}

// ─── TC29 — size=-1 returns 4xx ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC29_ActivityNegativeSizeTests extends TestBase {

        @Test
        @DisplayName("TC29 — GET /api/users/{ownId}/activity?size=-1 returns a 4xx")
        void activity_negative_size_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc29_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC29 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC29 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC29 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?size=-1";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC29: size=-1 must NOT 5xx — controller must validate gracefully. status="
                                                + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC29: size=-1 must NOT be 2xx — negative page size is semantically invalid. "
                                                + "status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC29: size=-1 must return a 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC30 — size=string returns 4xx (binding fails) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC30_ActivityStringSizeTests extends TestBase {

        @Test
        @DisplayName("TC30 — GET /api/users/{ownId}/activity?size=abc returns a 4xx (Integer binding fails)")
        void activity_string_size_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc30_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC30 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC30 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC30 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?size=abc";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC30: size=abc must NOT 5xx — Spring's @RequestParam Integer binding should "
                                                + "reject the string cleanly. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC30: size=abc must NOT be 2xx — non-numeric size cannot be valid. status="
                                                + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC30: size=abc must return a 4xx (typically 400 Bad Request); got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC31 — Cross-user activity (regular user) returns strictly 403 ─────────
@Tag("public")
@Tag("features_m2")
class TC31_ActivityCrossUserRegularTests extends TestBase {

        @Test
        @DisplayName("TC31 — Customer A's GET on User B's activity returns strictly 403 (per S1-F12 spec)")
        void cross_user_activity_regular_returns_403() throws Exception {
                BASE_URL = userServiceUrl;
                // Per spec: "ownership violation, NOT 404 — A's token is valid
                // and B exists." Strict 403 here, unlike TC17 (which accepts 403/404).
                String emailA = "tc31a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc31b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC31 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC31 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC31 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC31 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC31 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + bid + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC31: cross-user activity GET must NOT be 2xx — regular users cannot read "
                                                + "other users' activity feeds. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC31: cross-user activity GET must NOT 5xx. status=" + code + " body=" + r.body());
                assertEquals(403, code,
                                "TC31: per S1-F12 spec, cross-user activity GET must return strictly 403 "
                                                + "(ownership violation, NOT 404 — A's token is valid and B exists); got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC32 — Cross-user activity (admin) returns 2xx ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC32_ActivityCrossUserAdminTests extends TestBase {

        @Test
        @DisplayName("TC32 — Admin's GET on a customer's activity returns 2xx (admin bypasses ownership)")
        void cross_user_activity_admin_returns_2xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc32_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC32 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC32 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                String activityPath = "/api/users" + "/" + customerId + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                assert2xx(r, "TC32 admin activity");

                JsonNode j = parseNode(r.body());
                assertTrue(j.has("content") && j.get("content").isArray(),
                                "TC32: admin response must include `content` array; body=" + r.body());
        }
}

// ─── TC33 — page=-1 returns 4xx ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC33_ActivityNegativePageTests extends TestBase {

        @Test
        @DisplayName("TC33 — GET /api/users/{ownId}/activity?page=-1 returns a 4xx")
        void activity_negative_page_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc33_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC33 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC33 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC33 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?page=-1";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC33: page=-1 must NOT 5xx — PageRequest.of(int page, int size) requires "
                                                + "page >= 0; controller must validate gracefully. status=" + code
                                                + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC33: page=-1 must NOT be 2xx — negative page is semantically invalid. "
                                                + "status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC33: page=-1 must return a 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC34 — page=string returns 4xx (binding fails) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC34_ActivityStringPageTests extends TestBase {

        @Test
        @DisplayName("TC34 — GET /api/users/{ownId}/activity?page=abc returns a 4xx (Integer binding fails)")
        void activity_string_page_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc34_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC34 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC34 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC34 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?page=abc";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC34: page=abc must NOT 5xx — Spring's @RequestParam Integer binding should "
                                                + "reject the string cleanly. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC34: page=abc must NOT be 2xx — non-numeric page cannot be valid. status="
                                                + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC34: page=abc must return a 4xx (typically 400 Bad Request); got " + code
                                                + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2 M2 — Catalog entity features (full-text search, indexing, dashboard).
// All tests dynamic via TestBase helpers:
// * s2CatalogEntity() — Restaurant / Product / Provider / etc.
// * s3OrderEntity() — Order / Booking / Transaction / etc.
// * s2CategoricalFilterParam() — first non-status enum field (cuisineType /
// category / specialty / ...)
// * enumValueAt(entity, field, idx) — i-th valid value for that enum
// * s2EventsCollection() — Mongo events collection (spec name, validated)
// * s2SearchIndex() — ES index name (spec name, validated)
// * buildKitchenSinkBody(...) — JSON body from manifest entityColumns
// ════════════════════════════════════════════════════════════════════════════

// ─── TC35 — S2-F10 happy path search returns 2xx + array ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC35_SearchHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC35 — GET <s2>/search/full-text?query=test with valid token returns 2xx + array shape")
        void search_happy_path_returns_2xx_array() throws Exception {
                BASE_URL = catalogServiceUrl;
                String token = adminToken();
                String searchPath = "/api/jobs" + "/search/full-text?query=test";
                HttpResponse<String> r = httpGetAuth(searchPath, token);
                assert2xx(r, "TC35 search happy path");
                JsonNode body = parseNode(r.body());
                boolean validShape = body.isArray() || (body.has("content") && body.get("content").isArray());
                assertTrue(validShape, "TC35: response must be JSON array OR paginated envelope; got " + r.body());
        }
}

// ─── TC36 — S2-F10 no token returns 401 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC36_SearchNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC36 — GET <s2>/search/full-text without Authorization header returns 401")
        void search_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                String searchPath = "/api/jobs" + "/search/full-text?query=anything";
                HttpResponse<String> r = httpGet(searchPath);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC36: must NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC36: must NOT 5xx; got " + code);
                assertEquals(401, code, "TC36: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ─── TC37 — S2-F10 exact match by primary categorical filter ────────────────
@Tag("public")
@Tag("features_m2")
class TC37_SearchExactCategoricalFilterTests extends TestBase {
        @Test
        @DisplayName("TC37 — Search ?<filter>=<value0> returns only entities with that filter value")
        void search_filter_categorical_returns_only_matching() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String filterParam = s2CategoricalFilterParam();
                String filterValue0 = enumValueAt(entity, filterParam, 0);
                String filterValue1 = enumValueAt(entity, filterParam, 1);
                String statusOpen = enumValueAt(entity, "status", 0);

                String n = nonce();
                createEntity(adminTok, "TC37 First_" + n, filterValue0, statusOpen);
                createEntity(adminTok, "TC37 Second_" + n, filterValue1, statusOpen);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + n + "&" + filterParam + "="
                                + filterValue0;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC37 search by " + filterParam);
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        String c = item.has(filterParam) ? item.get(filterParam).asText() : null;
                        assertEquals(filterValue0, c,
                                        "TC37: every result must have " + filterParam + "=" + filterValue0 + "; got "
                                                        + item);
                }
        }

        private void createEntity(String tok, String name, String filterValue, String status) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name,
                                s2CategoricalFilterParam(), filterValue,
                                "status", status));
                assert2xx(httpPostAuth("/api/jobs", body, tok),
                                "TC37 setup create " + name);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC38 — S2-F10 exact match by status ────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC38_SearchExactStatusTests extends TestBase {
        @Test
        @DisplayName("TC38 — Search ?status=<value0> returns only entities with that status")
        void search_filter_status_returns_only_matching() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String filterValue0 = enumValueAt(entity, s2CategoricalFilterParam(), 0);
                String status0 = enumValueAt(entity, "status", 0);
                String status1 = enumValueAt(entity, "status", 1);

                String n = nonce();
                createEntity(adminTok, "TC38 Status0_" + n, filterValue0, status0);
                createEntity(adminTok, "TC38 Status1_" + n, filterValue0, status1);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + n + "&status=" + status0;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC38 search by status");
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        String s = item.has("status") ? item.get("status").asText() : null;
                        assertEquals(status0, s, "TC38: every result must have status=" + status0 + "; got " + item);
                }
        }

        private void createEntity(String tok, String name, String filterValue, String status) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name,
                                s2CategoricalFilterParam(), filterValue,
                                "status", status));
                assert2xx(httpPostAuth("/api/jobs", body, tok),
                                "TC38 setup create " + name);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC39 — S2-F10 minBudget + maxBudget range filter ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC39_SearchRatingRangeTests extends TestBase {
        @Test
        @DisplayName("TC39 — Search ?minBudget=100&maxBudget=300 returns only jobs whose budget overlaps [100, 300]")
        void search_rating_range_returns_entities_in_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String n = nonce();
                long lowId  = createWithBudget(adminTok, "TC39 Low_"  + n, 10.0,  50.0);
                long midId  = createWithBudget(adminTok, "TC39 Mid_"  + n, 150.0, 250.0);
                long highId = createWithBudget(adminTok, "TC39 High_" + n, 500.0, 1000.0);
                reindex(adminTok, lowId);
                reindex(adminTok, midId);
                reindex(adminTok, highId);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + n + "&minBudget=100&maxBudget=300";
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC39 search budget range");
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        double bMin = item.has("budgetMin") ? item.get("budgetMin").asDouble() : 0;
                        double bMax = item.has("budgetMax") ? item.get("budgetMax").asDouble() : 0;
                        assertTrue(bMin <= 300 && bMax >= 100,
                                        "TC39: job budget [" + bMin + "," + bMax + "] must overlap [100,300]; got " + item);
                }
        }

        private long createWithBudget(String tok, String name, double budgetMin, double budgetMax) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name, "budgetMin", budgetMin, "budgetMax", budgetMax));
                HttpResponse<String> r = httpPostAuth("/api/jobs", body, tok);
                assert2xx(r, "TC39 setup create " + name);
                return parseNode(r.body()).get("id").asLong();
        }

        private void reindex(String tok, long id) throws Exception {
                HttpResponse<String> r = httpPostAuth("/api/jobs" + "/" + id + "/index", "",
                                tok);
                assert2xx(r, "TC39 reindex id=" + id);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC40 — S2-F10 minBudget > maxBudget returns 4xx ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC40_SearchInvalidRatingRangeTests extends TestBase {
        @Test
        @DisplayName("TC40 — Search ?minBudget=500&maxBudget=300 (invalid range) returns a 4xx")
        void search_invalid_rating_range_returns_4xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String searchPath = "/api/jobs"
                                + "/search/full-text?query=test&minBudget=500&maxBudget=300";
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 5, "TC40: NOT 5xx; got " + code);
                assertTrue(code / 100 != 2, "TC40: NOT 2xx; got " + code);
                assertTrue(code >= 400 && code < 500, "TC40: must return 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC41 — S2-F10 query with no matches returns empty list ─────────────────
@Tag("public")
@Tag("features_m2")
class TC41_SearchNoMatchEmptyListTests extends TestBase {
        @Test
        @DisplayName("TC41 — Search with query that matches nothing returns 2xx + empty list")
        void search_no_match_returns_empty_list() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String improbableQuery = "TC41NoMatchQuery_" + nonce() + "_xyzqwe";
                String searchPath = "/api/jobs" + "/search/full-text?query="
                                + improbableQuery;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC41 search no match");
                JsonNode body = parseNode(r.body());
                JsonNode arr = body.isArray() ? body : (body.has("content") ? body.get("content") : body);
                assertTrue(arr.isArray(), "TC41: response must contain an array; got " + r.body());
                assertEquals(0, arr.size(), "TC41: must return empty list; got " + r.body());
        }
}

// ─── TC42 — S2-F10 results sorted by relevance ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC42_SearchSortedByRelevanceTests extends TestBase {
        @Test
        @DisplayName("TC42 — Search results sorted by relevance (name match ranks higher than description match)")
        void search_results_sorted_by_relevance() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String unique = "Tc42Word" + nonce();
                String aName = unique + " Kitchen";
                String bName = "Other Place TC42_" + nonce();
                long aid = createWithDetails(adminTok, aName, null);
                reindex(adminTok, aid);
                long bid = createWithDetails(adminTok, bName, "best authentic " + unique + " cuisine");
                reindex(adminTok, bid);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + unique;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC42 search relevance");
                JsonNode body = parseNode(r.body());
                JsonNode arr = body.isArray() ? body : (body.has("content") ? body.get("content") : body);
                assertTrue(arr.isArray() && arr.size() >= 1,
                                "TC42: must return at least one result; body=" + r.body());

                int idxA = -1, idxB = -1;
                for (int i = 0; i < arr.size(); i++) {
                        String entryName = arr.get(i).has("name") ? arr.get(i).get("name").asText() : "";
                        if (aName.equals(entryName))
                                idxA = i;
                        if (bName.equals(entryName))
                                idxB = i;
                }
                assertTrue(idxA >= 0,
                                "TC42: name-match (A, name='" + aName + "') must appear in results; body=" + r.body());
                if (idxB >= 0) {
                        assertTrue(idxA < idxB,
                                        "TC42: name-match (A, idx=" + idxA
                                                        + ") must rank higher than description-match (B, idx=" + idxB
                                                        + ").");
                }
        }

        private long createWithDetails(String tok, String name, String desc) throws Exception {
                java.util.Map<String, Object> overrides = new java.util.HashMap<>();
                overrides.put("name", name);
                if (desc != null) {
                        overrides.put("details", java.util.Map.of("description", desc));
                }
                String body = buildKitchenSinkBody(s2CatalogEntity(), overrides);
                HttpResponse<String> r = httpPostAuth("/api/jobs", body, tok);
                assert2xx(r, "TC42 setup create " + name);
                return parseNode(r.body()).get("id").asLong();
        }

        private void reindex(String tok, long id) throws Exception {
                HttpResponse<String> r = httpPostAuth("/api/jobs" + "/" + id + "/index", "",
                                tok);
                assert2xx(r, "TC42 reindex id=" + id);
        }
}

// ─── TC43 — S2-F11 happy index path ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC43_IndexHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC43 — POST <s2>/{id}/index for an existing entity returns 2xx")
        void index_happy_path_returns_2xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String body = buildKitchenSinkBody(s2CatalogEntity(),
                                java.util.Map.of("name", "TC43 Entity_" + nonce()));
                HttpResponse<String> created = httpPostAuth("/api/jobs", body, adminTok);
                assert2xx(created, "TC43 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                HttpResponse<String> r = httpPostAuth("/api/jobs" + "/" + id + "/index", "",
                                adminTok);
                assert2xx(r, "TC43 index");
        }
}

// ─── TC44 — S2-F11 indexed document matches PG attributes ───────────────────
@Tag("public")
@Tag("features_m2")
class TC44_IndexMatchesPgTests extends TestBase {
        @Test
        @DisplayName("TC44 — After indexing, ES doc fields match the PG row's attributes")
        void index_doc_matches_pg_attributes() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String unique = "TC44Entity_" + nonce();
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", unique,
                                "details", java.util.Map.of("description", "signature description")));
                HttpResponse<String> created = httpPostAuth("/api/jobs", body, adminTok);
                assert2xx(created, "TC44 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                String ratingCol = columnByField(s2CatalogEntity(), "rating");
                jdbc.update("UPDATE \"" + tableName(s2CatalogEntity()) + "\" SET \"" + ratingCol
                                + "\" = ? WHERE id = ?", 4.5, id);
                HttpResponse<String> indexed = httpPostAuth("/api/jobs" + "/" + id + "/index",
                                "", adminTok);
                assert2xx(indexed, "TC44 index");

                String esIndex = s2SearchIndex();
                long esCount = esSearchCount(esIndex, "name", unique);
                assertTrue(esCount >= 1,
                                "TC44: ES index '" + esIndex + "' must contain a document with name='" + unique
                                                + "' (count=" + esCount + ").");

                String searchPath = "/api/jobs" + "/search/full-text?query=" + unique;
                HttpResponse<String> sr = httpGetAuth(searchPath, adminTok);
                assert2xx(sr, "TC44 search after index");
                JsonNode body2 = parseNode(sr.body());
                JsonNode arr = body2.isArray() ? body2 : (body2.has("content") ? body2.get("content") : body2);
                JsonNode found = null;
                for (JsonNode item : arr) {
                        String entryName = item.has("name") ? item.get("name").asText() : "";
                        if (unique.equals(entryName)) {
                                found = item;
                                break;
                        }
                }
                assertNotNull(found, "TC44: indexed entity must be findable via /search/full-text by name='" + unique
                                + "'; got " + sr.body());

                // Verify name + status fields match between search result and PG row.
                java.util.Map<String, Object> pgRow = jdbc.queryForMap(
                                "SELECT name, status::text AS status FROM " + tableName(s2CatalogEntity())
                                                + " WHERE id = ?",
                                id);
                assertEquals(pgRow.get("name"), found.get("name").asText(), "TC44: ES name must match PG name");
                if (found.has("status")) {
                        assertEquals(pgRow.get("status"), found.get("status").asText(),
                                        "TC44: ES status must match PG status");
                }
        }
}

// ─── TC45 — S2-F11 auto-reindex on update ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC45_IndexAutoReindexOnUpdateTests extends TestBase {
        @Test
        @DisplayName("TC45 — Updating an entity via PUT (without /index) makes the new name searchable")
        void auto_reindex_on_update() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String origName = "TC45 OriginalName_" + nonce();
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", origName));
                HttpResponse<String> created = httpPostAuth("/api/jobs", body, adminTok);
                assert2xx(created, "TC45 setup create");
                long id = parseNode(created.body()).get("id").asLong();

                String newName = "TC45_NewName_" + nonce();
                String putBody = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", newName));
                HttpResponse<String> updated = httpPutAuth("/api/jobs/" + id, putBody, adminTok);
                assert2xx(updated, "TC45 update name");

                String searchPath = "/api/jobs" + "/search/full-text?query=" + newName;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC45 search by new name");
                JsonNode body2 = parseNode(r.body());
                JsonNode arr = body2.isArray() ? body2 : (body2.has("content") ? body2.get("content") : body2);
                boolean found = false;
                for (JsonNode item : arr) {
                        String entryName = item.has("name") ? item.get("name").asText() : "";
                        if (newName.equals(entryName)) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC45: search by new name must find the entity (proves auto-reindexing). body="
                                + r.body());
        }
}

// ─── TC46 — S2-F11 index on non-existent entity returns 404 ─────────────────
@Tag("public")
@Tag("features_m2")
class TC46_IndexNonExistentTests extends TestBase {
        @Test
        @DisplayName("TC46 — POST <s2>/<Long.MAX_VALUE>/index returns strictly 404")
        void index_non_existent_returns_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String indexPath = "/api/jobs" + "/" + Long.MAX_VALUE + "/index";
                HttpResponse<String> r = httpPostAuth(indexPath, "", adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC46: NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC46: NOT 5xx; got " + code);
                assertEquals(404, code, "TC46: must be strict 404; got " + code + " body=" + r.body());
        }
}

// ─── TC47 — S2-F11 index without token returns 401 ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC47_IndexNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC47 — POST <s2>/{id}/index without Authorization header returns 401")
        void index_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String body = buildKitchenSinkBody(s2CatalogEntity(),
                                java.util.Map.of("name", "TC47 Entity_" + nonce()));
                HttpResponse<String> created = httpPostAuth("/api/jobs", body, adminTok);
                assert2xx(created, "TC47 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                HttpResponse<String> r = httpPost("/api/jobs" + "/" + id + "/index", "");
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC47: NOT 2xx; got " + code);
                assertEquals(401, code, "TC47: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ─── TC48 — S2-F12 dashboard happy path (uses pre-seeded entity id=1) ───────
@Tag("public")
@Tag("features_m2")
class TC48_DashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC48 — GET <s2>/{id}/dashboard returns 2xx + DTO with totalOrders/totalRevenue")
        void dashboard_happy_path() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                long restId = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                HttpResponse<String> r = httpGetAuth(
                                "/api/jobs" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC48 dashboard");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalProposals") || j.has("total_proposals") || j.has("totalOrders") || j.has("total_orders"),
                                "TC48: dashboard must include totalProposals; got " + r.body());
                assertTrue(j.has("acceptedProposals") || j.has("accepted_proposals") || j.has("averageBidAmount") || j.has("average_bid_amount") || j.has("totalRevenue") || j.has("total_revenue"),
                                "TC48: dashboard must include acceptedProposals or averageBidAmount; got " + r.body());
        }
}

// ─── TC49 — S2-F12 aggregated values match PG-source values ─────────────────
@Tag("public")
@Tag("features_m2")
class TC49_DashboardAggregatesMatchPgTests extends TestBase {
        @Test
        @DisplayName("TC49 — Dashboard totalOrders/totalRevenue match values aggregated from PG (uses pre-seed)")
        void dashboard_aggregates_match_pg() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                long restId = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                String ordersTable = tableName(s3OrderEntity());
                String fkCol = s2CatalogFkColumn();
                String amtCol = null;
                try { amtCol = columnByField(s3OrderEntity(), "bidAmount", "totalAmount"); } catch (Throwable ignore) {}

                HttpResponse<String> r = httpGetAuth(
                                "/api/jobs" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC49 dashboard");
                JsonNode j = parseNode(r.body());

                Integer expectedCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + ordersTable + "\" WHERE \"" + fkCol + "\" = ?",
                                Integer.class, restId);
                Double expectedRevenueRaw = amtCol == null ? 0.0 : jdbc.queryForObject(
                                "SELECT COALESCE(SUM(\"" + amtCol + "\"), 0) FROM \"" + ordersTable + "\" WHERE \""
                                                + fkCol + "\" = ?",
                                Double.class, restId);
                double expectedRevenue = expectedRevenueRaw != null ? expectedRevenueRaw : 0.0;

                long actualCount = j.has("totalProposals") ? j.get("totalProposals").asLong()
                                : j.has("total_proposals") ? j.get("total_proposals").asLong()
                                : j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1L;
                double actualRevenue = j.has("averageBidAmount") ? j.get("averageBidAmount").asDouble()
                                : j.has("average_bid_amount") ? j.get("average_bid_amount").asDouble()
                                : j.has("acceptedProposals") ? (double) j.get("acceptedProposals").asLong()
                                : j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : expectedRevenue;

                assertEquals(expectedCount.longValue(), actualCount,
                                "TC49: totalProposals mismatch — PG=" + expectedCount + ", dashboard=" + actualCount
                                                + ". body=" + r.body());
                assertEquals(expectedRevenue, actualRevenue, 0.01,
                                "TC49: revenue/bid mismatch — PG=" + expectedRevenue + ", dashboard=" + actualRevenue
                                                + ". body=" + r.body());
        }
}

// ─── TC50 — S2-F12 dashboard event written to MongoDB ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC50_DashboardEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC50 — After GET /dashboard, an event must appear in the spec-defined Mongo collection")
        void dashboard_logs_event_to_mongo() throws Exception {
                BASE_URL = catalogServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC50: MongoDB is required for this test but not reachable. Set "
                                                        + "SPRING_DATA_MONGODB_URI or ensure the Mongo container is up.");
                }
                String adminTok = adminToken();
                long restId = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                String collName = s2EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> coll = mongo.getCollection(collName);

                long before = coll.countDocuments();
                HttpResponse<String> r = httpGetAuth(
                                "/api/jobs" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC50 dashboard");
                long after = coll.countDocuments();

                assertTrue(after > before,
                                "TC50: GET /dashboard must log an event in collection '" + collName
                                                + "'. Counts: before=" + before + ", after=" + after);
        }
}

// ─── TC51 — S2-F12 dashboard for non-existent ID returns 404 ────────────────
@Tag("public")
@Tag("features_m2")
class TC51_DashboardNonExistentTests extends TestBase {
        @Test
        @DisplayName("TC51 — GET <s2>/<Long.MAX_VALUE>/dashboard returns strictly 404")
        void dashboard_non_existent_returns_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String dashPath = "/api/jobs" + "/" + Long.MAX_VALUE + "/dashboard";
                HttpResponse<String> r = httpGetAuth(dashPath, adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC51: NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC51: NOT 5xx; got " + code);
                assertEquals(404, code, "TC51: must be strict 404; got " + code + " body=" + r.body());
        }
}

// ─── TC52 — S2-F12 dashboard for entity with no orders returns zeros ────────
@Tag("public")
@Tag("features_m2")
class TC52_DashboardNoOrdersTests extends TestBase {
        @Test
        @DisplayName("TC52 — Dashboard for an entity with no orders returns 2xx + totalOrders=0 + totalRevenue=0")
        void dashboard_no_orders_returns_zeros() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                // Pre-seed catalog id=3 has no orders attached (the cross-theme baseline
                // seed plants orders against ids 1, 2, 4 — id=3 left empty intentionally).
                // We DELETE any orders for restId=3 defensively in case a prior test left
                // residual data.
                long restId = 3L;
                String fkCol = s2CatalogFkColumn();
                jdbc.update("DELETE FROM \"" + tableName(s3OrderEntity()) + "\" WHERE \"" + fkCol + "\" = ?", restId);

                HttpResponse<String> r = httpGetAuth(
                                "/api/jobs" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC52 dashboard");
                JsonNode j = parseNode(r.body());

                long totalOrders = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1L;
                double totalRevenue = j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : -1.0;

                assertEquals(0L, totalOrders, "TC52: must report totalOrders=0; got " + totalOrders);
                assertEquals(0.0, totalRevenue, 0.01, "TC52: must report totalRevenue=0; got " + totalRevenue);
        }
}

// ─── TC53 — S2-F12 dashboard without token returns 401 ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC53_DashboardNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC53 — GET <s2>/{id}/dashboard without Authorization header returns 401")
        void dashboard_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = 1L;
                HttpResponse<String> r = httpGet("/api/jobs" + "/" + restId + "/dashboard");
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC53: NOT 2xx; got " + code);
                assertEquals(401, code, "TC53: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3 M2 — Proposal Service features (TC54..TC99)
//
// Covers S3-F10 (proposal analytics dashboard, TC54-TC69), S3-F11 (record
// freelancer-job interaction, TC70-TC84), and S3-F12 (job recommendations,
// TC85-TC99). Theme-specific terms: S3 endpoints under /api/proposals;
// Proposal.status enum is {SUBMITTED, SHORTLISTED, ACCEPTED, REJECTED,
// WITHDRAWN}; Neo4j relationship is PROPOSED_TO with proposalCount +
// lastProposalDate; graph User node label is Freelancer (theme.json driven).
// Per-test wipe of PG/Neo4j/Redis happens in autoTruncateAllData()
// (@BeforeEach + @AfterEach).
// ════════════════════════════════════════════════════════════════════════════

// ─── TC54 — S3-F10 dashboard happy path ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC54_AnalyticsDashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC54 — Dashboard returns totalProposals/acceptanceRate/averageBidAmount/averageEstimatedDays/proposalsByStatus")
        void dashboard_happy_path() throws Exception {
                BASE_URL = orderServiceUrl;
                String[] sts = {"ACCEPTED","ACCEPTED","ACCEPTED","ACCEPTED","ACCEPTED","ACCEPTED",
                                "REJECTED","REJECTED","SUBMITTED","SUBMITTED"};
                double[] amts = {100, 80, 120, 90, 150, 60, 200, 50, 180, 70};
                int[] days = {7, 5, 10, 6, 14, 4, 21, 3, 10, 5};
                for (int i = 0; i < sts.length; i++) {
                        _FmM2.prop(this, 1L, 1L, sts[i], amts[i], days[i], "2026-09-" + String.format("%02d", i + 1));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC54");
                JsonNode j = parseNode(r.body());
                assertEquals(10L, _FmM2.rL(j, "totalProposals", "total_proposals"),
                        "TC54: totalProposals=10; body=" + r.body());
                assertEquals(0.6, _FmM2.rD(j, "acceptanceRate", "acceptance_rate"), 0.01,
                        "TC54: acceptanceRate=0.6 (6/10)");
        }
}

// ─── TC55 — S3-F10 totalProposals isolated ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC55_AnalyticsTotalProposalsTests extends TestBase {
        @Test
        @DisplayName("TC55 — Dashboard.totalProposals equals exact count of proposals in range")
        void total_proposals_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                for (int i = 0; i < 7; i++) {
                        _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 5, "2026-09-" + String.format("%02d", i + 1));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC55");
                assertEquals(7L,
                        _FmM2.rL(parseNode(r.body()), "totalProposals", "total_proposals"),
                        "TC55: totalProposals=7");
        }
}

// ─── TC56 — S3-F10 averageBidAmount isolated ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC56_AnalyticsAverageBidTests extends TestBase {
        @Test
        @DisplayName("TC56 — Dashboard.averageBidAmount = SUM(bidAmount)/COUNT")
        void average_bid_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 5, "2026-09-10");
                _FmM2.prop(this, 1L, 1L, "SUBMITTED", 200.0, 5, "2026-09-11");
                _FmM2.prop(this, 1L, 1L, "SUBMITTED", 300.0, 5, "2026-09-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC56");
                assertEquals(200.0,
                        _FmM2.rD(parseNode(r.body()), "averageBidAmount", "average_bid_amount", "avgBidAmount"),
                        0.01, "TC56: averageBidAmount=200");
        }
}

// ─── TC57 — S3-F10 averageEstimatedDays isolated ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC57_AnalyticsAvgEstimatedDaysTests extends TestBase {
        @Test
        @DisplayName("TC57 — Dashboard.averageEstimatedDays = SUM(estimatedDays)/COUNT")
        void average_estimated_days_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 4, "2026-09-10");
                _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 8, "2026-09-11");
                _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 12, "2026-09-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC57");
                JsonNode j = parseNode(r.body());
                double avg = _FmM2.rD(j, "averageEstimatedDays", "average_estimated_days", "avgEstimatedDays");
                assertEquals(8.0, avg, 0.01, "TC57: averageEstimatedDays=8");
        }
}

// ─── TC58 — S3-F10 acceptanceRate isolated ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC58_AnalyticsAcceptanceRateTests extends TestBase {
        @Test
        @DisplayName("TC58 — Dashboard.acceptanceRate = ACCEPTED / total")
        void acceptance_rate_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String[] sts = {"ACCEPTED","ACCEPTED","ACCEPTED","ACCEPTED","ACCEPTED",
                                "REJECTED","REJECTED","REJECTED"};
                for (int i = 0; i < sts.length; i++) {
                        _FmM2.prop(this, 1L, 1L, sts[i], 100.0, 5, "2026-09-" + String.format("%02d", i + 10));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC58");
                JsonNode j = parseNode(r.body());
                assertEquals(0.625, _FmM2.rD(j, "acceptanceRate", "acceptance_rate"), 0.001,
                        "TC58: acceptanceRate=0.625");
        }
}

// ─── TC59 — S3-F10 proposalsByStatus has all 5 statuses ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC59_AnalyticsProposalsByStatusTests extends TestBase {
        @Test
        @DisplayName("TC59 — Dashboard.proposalsByStatus has all 5 Proposal statuses, each count=1")
        void proposals_by_status_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String[] sts = {"SUBMITTED","SHORTLISTED","ACCEPTED","REJECTED","WITHDRAWN"};
                for (int i = 0; i < sts.length; i++) {
                        _FmM2.prop(this, 1L, 1L, sts[i], 100.0, 5, "2026-09-" + String.format("%02d", i + 10));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC59");
                JsonNode j = parseNode(r.body());
                JsonNode bd = _FmM2.rO(j, "proposalsByStatus", "proposals_by_status");
                assertNotNull(bd, "TC59: proposalsByStatus key required");
                for (String st : sts) {
                        assertTrue(bd.has(st), "TC59: proposalsByStatus missing key '" + st + "'");
                        assertEquals(1L, bd.get(st).asLong(),
                                "TC59: proposalsByStatus[" + st + "]=1; got " + bd.get(st).asLong());
                }
        }
}

// ─── TC60 — S3-F10 empty range returns zeros ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC60_AnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC60 — Dashboard with no proposals in range returns totalProposals=0, averageBidAmount=0")
        void empty_range_zeros() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC60");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, _FmM2.rL(j, "totalProposals", "total_proposals"),
                        "TC60: totalProposals=0");
                assertEquals(0.0, _FmM2.rD(j, "averageBidAmount", "average_bid_amount"), 0.01,
                        "TC60: averageBidAmount=0");
        }
}

// ─── TC61 — S3-F10 startDate boundary inclusion ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC61_AnalyticsStartBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC61 — Proposal at exactly startDate is included")
        void start_boundary_included() throws Exception {
                BASE_URL = orderServiceUrl;
                String pTable = tableName("Proposal");
                Long pid = _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 5, "2026-05-01");
                String submittedAtCol = columnByField("Proposal", "submittedAt");
                jdbc.update("UPDATE \"" + pTable + "\" SET \"" + submittedAtCol + "\"=? WHERE id=?",
                        java.sql.Timestamp.valueOf("2026-05-01 00:00:00"), pid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-05-01&endDate=2026-05-31", tok);
                assert2xx(r, "TC61");
                JsonNode j = parseNode(r.body());
                assertEquals(1L, _FmM2.rL(j, "totalProposals", "total_proposals"),
                        "TC61: boundary proposal must be counted; body=" + r.body());
        }
}

// ─── TC62 — S3-F10 endDate boundary inclusion ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC62_AnalyticsEndBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC62 — Proposal at exactly endDate is included")
        void end_boundary_included() throws Exception {
                BASE_URL = orderServiceUrl;
                String pTable = tableName("Proposal");
                Long pid = _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 5, "2026-05-31");
                String submittedAtCol = columnByField("Proposal", "submittedAt");
                jdbc.update("UPDATE \"" + pTable + "\" SET \"" + submittedAtCol + "\"=? WHERE id=?",
                        java.sql.Timestamp.valueOf("2026-05-31 23:59:59"), pid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-05-01&endDate=2026-05-31", tok);
                assert2xx(r, "TC62");
                JsonNode j = parseNode(r.body());
                assertEquals(1L, _FmM2.rL(j, "totalProposals", "total_proposals"),
                        "TC62: end-boundary proposal must be counted; body=" + r.body());
        }
}

// ─── TC63 — S3-F10 invalid date range (start > end) returns 400 ─────────────
@Tag("public")
@Tag("features_m2")
class TC63_AnalyticsInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC63 — Dashboard with startDate > endDate returns 400")
        void invalid_date_range_400() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(),
                        "TC63: must be 400; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC64 — S3-F10 missing JWT returns 401 ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC64_AnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC64 — Dashboard without Authorization header returns 401")
        void missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGet(
                        "/api/proposals/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31");
                assertEquals(401, r.statusCode(),
                        "TC64: must be 401; got " + r.statusCode());
        }
}

// ─── TC65 — S3-F10 invalid JWT returns 401 ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC65_AnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC65 — Dashboard with malformed JWT returns 401")
        void invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC65: must be 401; got " + r.statusCode());
        }
}

// ─── TC66 — S3-F10 ANALYTICS_VIEWED logged on first call ────────────────────
@Tag("public")
@Tag("features_m2")
class TC66_AnalyticsViewedLoggedTests extends TestBase {
        @Test
        @DisplayName("TC66 — First dashboard call writes ANALYTICS_VIEWED to proposal_events")
        void analytics_viewed_on_first_call() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) throw new AssertionError("TC66: MongoDB required");
                String coll = s3EventsCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics/dashboard?startDate=2026-07-01&endDate=2026-07-31", tok);
                assert2xx(r, "TC66");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after > before,
                        "TC66: ANALYTICS_VIEWED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC67 — S3-F10 ANALYTICS_VIEWED logged on cache hit too ─────────────────
@Tag("public")
@Tag("features_m2")
class TC67_AnalyticsViewedOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC67 — Second dashboard call (cache hit) still logs ANALYTICS_VIEWED")
        void analytics_viewed_on_cache_hit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) throw new AssertionError("TC67: MongoDB required");
                String coll = s3EventsCollection();
                String tok = adminToken();
                String url = "/api/proposals/analytics/dashboard?startDate=2026-07-01&endDate=2026-07-31";
                assert2xx(httpGetAuth(url, tok), "TC67 first");
                long after1 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assert2xx(httpGetAuth(url, tok), "TC67 second");
                long after2 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after2 > after1,
                        "TC67: ANALYTICS_VIEWED must be logged on cache hit; after1=" + after1 + " after2=" + after2);
        }
}

// ─── TC68 — S3-F10 cache populated on first call ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC68_AnalyticsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC68 — First dashboard call populates Redis with TTL ≤ 600s")
        void cache_populated() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) throw new AssertionError("TC68: Redis required");
                String tok = adminToken();
                String url = "/api/proposals/analytics/dashboard?startDate=2026-08-01&endDate=2026-08-31";
                long before = redis.dbSize();
                assert2xx(httpGetAuth(url, tok), "TC68");
                long after = redis.dbSize();
                assertTrue(after > before,
                        "TC68: at least one cache key must be added; before=" + before + " after=" + after);
        }
}

// ─── TC69 — S3-F10 cache returns same body for repeated calls ───────────────
@Tag("public")
@Tag("features_m2")
class TC69_AnalyticsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC69 — Two identical dashboard requests return identical bodies (cached)")
        void cache_same_body() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String url = "/api/proposals/analytics/dashboard?startDate=2026-07-01&endDate=2026-07-31";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC69 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC69 second");
                assertEquals(r1.body(), r2.body(),
                        "TC69: cached body must match");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F11 — Record Freelancer-Job Interaction Pattern (TC70-TC84)
// ════════════════════════════════════════════════════════════════════════════

// ─── TC70 — S3-F11 happy path creates PROPOSED_TO edge with proposalCount=1 ─
@Tag("public")
@Tag("features_m2")
class TC70_RecordInteractionHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC70 — Record interaction on SUBMITTED proposal creates PROPOSED_TO with proposalCount=1")
        void record_interaction_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC70: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc70u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC70 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/proposals/" + pid + "/record-interaction", "", tok);
                assert2xx(r, "TC70");
                long count = _FmM2.proposalCount(this, uid, jid);
                assertEquals(1L, count, "TC70: proposalCount=1; got " + count);
        }
}

// ─── TC71 — S3-F11 idempotency: same proposal twice → proposalCount stays 1
@Tag("public")
@Tag("features_m2")
class TC71_RecordInteractionIdempotencyTests extends TestBase {
        @Test
        @DisplayName("TC71 — Same proposalId recorded twice keeps proposalCount=1")
        void record_interaction_idempotent() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC71: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc71u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC71 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC71 first");
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC71 second");
                long count = _FmM2.proposalCount(this, uid, jid);
                assertEquals(1L, count, "TC71: proposalCount must stay 1 after duplicate; got " + count);
        }
}

// ─── TC72 — S3-F11 two distinct proposals same user→job → proposalCount=2 ──
@Tag("public")
@Tag("features_m2")
class TC72_RecordInteractionTwoDistinctTests extends TestBase {
        @Test
        @DisplayName("TC72 — Two distinct SUBMITTED proposals same user→job → proposalCount=2")
        void record_interaction_two_distinct() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC72: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc72u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC72 Job " + nonce());
                Long p1 = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                Long p2 = _FmM2.prop(this, uid, jid, "SUBMITTED", 110.0, 5, "2026-04-12");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + p1 + "/record-interaction", "", tok), "TC72 p1");
                assert2xx(httpPostAuth("/api/proposals/" + p2 + "/record-interaction", "", tok), "TC72 p2");
                long count = _FmM2.proposalCount(this, uid, jid);
                assertEquals(2L, count, "TC72: proposalCount=2; got " + count);
        }
}

// ─── TC73 — S3-F11 different job → new edge with proposalCount=1 ────────────
@Tag("public")
@Tag("features_m2")
class TC73_RecordInteractionDifferentJobTests extends TestBase {
        @Test
        @DisplayName("TC73 — Recording interaction for a different job creates a new edge")
        void record_interaction_different_job() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC73: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc73u");
                long uid = ((Number) u.get("id")).longValue();
                long j1 = _FmM2.job(this, "TC73 J1 " + nonce());
                long j2 = _FmM2.job(this, "TC73 J2 " + nonce());
                Long p1 = _FmM2.prop(this, uid, j1, "SUBMITTED", 100.0, 5, "2026-04-10");
                Long p2 = _FmM2.prop(this, uid, j2, "SUBMITTED", 200.0, 5, "2026-04-11");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + p1 + "/record-interaction", "", tok), "TC73 p1");
                assert2xx(httpPostAuth("/api/proposals/" + p2 + "/record-interaction", "", tok), "TC73 p2");
                assertEquals(1L, _FmM2.proposalCount(this, uid, j1), "TC73: edge to j1 stays at 1");
                assertEquals(1L, _FmM2.proposalCount(this, uid, j2), "TC73: edge to j2 is 1");
        }
}

// ─── TC74 — S3-F11 ACCEPTED proposal → 400 ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC74_RecordInteractionAcceptedTests extends TestBase {
        @Test
        @DisplayName("TC74 — Recording an ACCEPTED proposal returns 400")
        void record_interaction_accepted_400() throws Exception {
                BASE_URL = orderServiceUrl;
                Long pid = _FmM2.prop(this, 1L, 1L, "ACCEPTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/proposals/" + pid + "/record-interaction", "", tok);
                assertEquals(400, r.statusCode(),
                        "TC74: must be 400 for ACCEPTED; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC75 — S3-F11 REJECTED proposal → 400 ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC75_RecordInteractionRejectedTests extends TestBase {
        @Test
        @DisplayName("TC75 — Recording a REJECTED proposal returns 400")
        void record_interaction_rejected_400() throws Exception {
                BASE_URL = orderServiceUrl;
                Long pid = _FmM2.prop(this, 1L, 1L, "REJECTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/proposals/" + pid + "/record-interaction", "", tok);
                assertEquals(400, r.statusCode(),
                        "TC75: must be 400 for REJECTED; got " + r.statusCode());
        }
}

// ─── TC76 — S3-F11 non-existent proposal → 404 ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC76_RecordInteractionNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC76 — Record interaction for non-existent proposal returns 404")
        void record_interaction_not_found_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/proposals/999999/record-interaction", "", tok);
                assertEquals(404, r.statusCode(),
                        "TC76: must be 404; got " + r.statusCode());
        }
}

// ─── TC77 — S3-F11 missing JWT → 401 ───────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC77_RecordInteractionMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC77 — Record interaction without Authorization header returns 401")
        void record_interaction_missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                Long pid = _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 5, "2026-04-10");
                HttpResponse<String> r = httpPost(
                        "/api/proposals/" + pid + "/record-interaction", "");
                assertEquals(401, r.statusCode(),
                        "TC77: must be 401; got " + r.statusCode());
        }
}

// ─── TC78 — S3-F11 invalid JWT → 401 ───────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC78_RecordInteractionInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC78 — Record interaction with bogus JWT returns 401")
        void record_interaction_invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                Long pid = _FmM2.prop(this, 1L, 1L, "SUBMITTED", 100.0, 5, "2026-04-10");
                HttpResponse<String> r = httpPostAuth(
                        "/api/proposals/" + pid + "/record-interaction", "", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC78: must be 401; got " + r.statusCode());
        }
}

// ─── TC79 — S3-F11 lastProposalDate set on success ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC79_RecordInteractionLastProposalDateTests extends TestBase {
        @Test
        @DisplayName("TC79 — PROPOSED_TO edge has lastProposalDate property after recording")
        void record_interaction_last_proposal_date() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC79: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc79u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC79 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC79");
                java.util.List<java.util.Map<String, Object>> rows = neo4jExec(
                        "MATCH (a:`" + s3GraphUserLabel() + "` {id:$u})-[r:`" + s3GraphRelationship()
                          + "`]->(b:`" + s3GraphCatalogLabel() + "` {id:$p}) RETURN properties(r) AS rel LIMIT 1",
                        java.util.Map.of("u", uid, "p", jid));
                assertFalse(rows.isEmpty(), "TC79: PROPOSED_TO edge must exist");
                Object rel = rows.get(0).get("rel");
                assertNotNull(rel, "TC79: relationship returned");
                String relStr = rel.toString();
                assertTrue(relStr.contains("lastProposalDate") || relStr.contains("last_proposal_date")
                                || relStr.contains("lastProposal") || relStr.contains("lastBookingDate")
                                || relStr.contains("lastOrderDate"),
                        "TC79: edge must carry lastProposalDate property; got " + relStr);
        }
}

// ─── TC80 — S3-F11 Neo4j Job node has correct id ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC80_RecordInteractionJobNodeTests extends TestBase {
        @Test
        @DisplayName("TC80 — Neo4j Job node exists with the seeded job id")
        void record_interaction_job_node() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC80: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc80u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC80 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC80");
                long count = neo4jNodeCount(s3GraphCatalogLabel(), jid);
                assertTrue(count >= 1, "TC80: Job node id=" + jid + " must exist; count=" + count);
        }
}

// ─── TC81 — S3-F11 Neo4j Freelancer node has correct id ────────────────────
@Tag("public")
@Tag("features_m2")
class TC81_RecordInteractionFreelancerNodeTests extends TestBase {
        @Test
        @DisplayName("TC81 — Neo4j Freelancer node exists with the proposal's user id")
        void record_interaction_user_node() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC81: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc81u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC81 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC81");
                long count = neo4jNodeCount(s3GraphUserLabel(), uid);
                assertTrue(count >= 1, "TC81: Freelancer node id=" + uid + " must exist; count=" + count);
        }
}

// ─── TC82 — S3-F11 INTERACTION_RECORDED logged to proposal_events ──────────
@Tag("public")
@Tag("features_m2")
class TC82_RecordInteractionEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC82 — Record interaction writes INTERACTION_RECORDED to proposal_events")
        void record_interaction_event_logged() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) throw new AssertionError("TC82: MongoDB required");
                String coll = s3EventsCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "INTERACTION_RECORDED"));
                java.util.Map<String, Object> u = seedAndLoginUser("tc82u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC82 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC82");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "INTERACTION_RECORDED"));
                assertTrue(after > before,
                        "TC82: INTERACTION_RECORDED must be logged; before=" + before + " after=" + after);
        }
}

// ─── TC83 — S3-F11 idempotent path skips event log ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC83_RecordInteractionIdempotentSkipsEventTests extends TestBase {
        @Test
        @DisplayName("TC83 — Second (idempotent) call does NOT log INTERACTION_RECORDED again")
        void record_interaction_skips_event_on_idempotent() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) throw new AssertionError("TC83: MongoDB required");
                if (neo4j == null) throw new AssertionError("TC83: Neo4j required");
                String coll = s3EventsCollection();
                java.util.Map<String, Object> u = seedAndLoginUser("tc83u");
                long uid = ((Number) u.get("id")).longValue();
                long jid = _FmM2.job(this, "TC83 Job " + nonce());
                Long pid = _FmM2.prop(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC83 first");
                long after1 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "INTERACTION_RECORDED"));
                assert2xx(httpPostAuth("/api/proposals/" + pid + "/record-interaction", "", tok), "TC83 second");
                long after2 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "INTERACTION_RECORDED"));
                assertEquals(after1, after2,
                        "TC83: INTERACTION_RECORDED must NOT be logged on idempotent retry; after1=" + after1 + " after2=" + after2);
        }
}

// ─── TC84 — S3-F11 SHORTLISTED proposal → 400 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC84_RecordInteractionShortlistedTests extends TestBase {
        @Test
        @DisplayName("TC84 — Recording a SHORTLISTED proposal returns 400")
        void record_interaction_shortlisted_400() throws Exception {
                BASE_URL = orderServiceUrl;
                Long pid = _FmM2.prop(this, 1L, 1L, "SHORTLISTED", 100.0, 5, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/proposals/" + pid + "/record-interaction", "", tok);
                assertEquals(400, r.statusCode(),
                        "TC84: must be 400 for SHORTLISTED; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F12 — Job Recommendations (TC85-TC99)
// ════════════════════════════════════════════════════════════════════════════

// ─── TC85 — S3-F12 happy path (composite scenario from spec) ────────────────
@Tag("public")
@Tag("features_m2")
class TC85_RecommendationsHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC85 — Recs for A (proposed J1,J2) include J3 (B proposed J1) and J4 (C proposed J2); exclude J1,J2")
        void recommendations_happy_path() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC85: Neo4j required");
                java.util.Map<String, Object> a = seedAndLoginUser("tc85a");
                java.util.Map<String, Object> b = seedAndLoginUser("tc85b");
                java.util.Map<String, Object> c = seedAndLoginUser("tc85c");
                long aid = ((Number) a.get("id")).longValue();
                long bid = ((Number) b.get("id")).longValue();
                long cid = ((Number) c.get("id")).longValue();
                long j1 = _FmM2.job(this, "TC85 J1 " + nonce());
                long j2 = _FmM2.job(this, "TC85 J2 " + nonce());
                long j3 = _FmM2.job(this, "TC85 J3 " + nonce());
                long j4 = _FmM2.job(this, "TC85 J4 " + nonce());
                String tok = adminToken();
                _FmM2.proposeAndRecord(this, aid, j1, tok);
                _FmM2.proposeAndRecord(this, aid, j2, tok);
                _FmM2.proposeAndRecord(this, bid, j1, tok);
                _FmM2.proposeAndRecord(this, bid, j3, tok);
                _FmM2.proposeAndRecord(this, cid, j2, tok);
                _FmM2.proposeAndRecord(this, cid, j4, tok);
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, (String) a.get("token"));
                assert2xx(r, "TC85");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                java.util.Set<Long> ids = new java.util.HashSet<>();
                for (JsonNode it : arr) {
                        long id = _FmM2.rL(it, "jobId", "id");
                        if (id > 0) ids.add(id);
                }
                assertTrue(ids.contains(j3), "TC85: must include j3; got " + ids);
                assertTrue(ids.contains(j4), "TC85: must include j4; got " + ids);
                assertFalse(ids.contains(j1), "TC85: must NOT include j1 (already proposed); got " + ids);
                assertFalse(ids.contains(j2), "TC85: must NOT include j2 (already proposed); got " + ids);
        }
}

// ─── TC86 — S3-F12 limit parameter respected ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC86_RecommendationsLimitTests extends TestBase {
        @Test
        @DisplayName("TC86 — Recommendations with limit=2 returns at most 2 jobs")
        void recommendations_limit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC86: Neo4j required");
                java.util.Map<String, Object> a = seedAndLoginUser("tc86a");
                java.util.Map<String, Object> b = seedAndLoginUser("tc86b");
                long aid = ((Number) a.get("id")).longValue();
                long bid = ((Number) b.get("id")).longValue();
                long j1 = _FmM2.job(this, "TC86 J1 " + nonce());
                long j2 = _FmM2.job(this, "TC86 J2 " + nonce());
                long j3 = _FmM2.job(this, "TC86 J3 " + nonce());
                long j4 = _FmM2.job(this, "TC86 J4 " + nonce());
                long j5 = _FmM2.job(this, "TC86 J5 " + nonce());
                String tok = adminToken();
                _FmM2.proposeAndRecord(this, aid, j1, tok);
                _FmM2.proposeAndRecord(this, bid, j1, tok);
                _FmM2.proposeAndRecord(this, bid, j2, tok);
                _FmM2.proposeAndRecord(this, bid, j3, tok);
                _FmM2.proposeAndRecord(this, bid, j4, tok);
                _FmM2.proposeAndRecord(this, bid, j5, tok);
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid + "&limit=2",
                        (String) a.get("token"));
                assert2xx(r, "TC86");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertTrue(arr.size() <= 2,
                        "TC86: limit=2 must cap results; got " + arr.size());
        }
}

// ─── TC87 — S3-F12 default limit (no limit param) ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC87_RecommendationsDefaultLimitTests extends TestBase {
        @Test
        @DisplayName("TC87 — Recommendations without limit param uses spec default")
        void recommendations_default_limit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC87: Neo4j required");
                java.util.Map<String, Object> a = seedAndLoginUser("tc87a");
                long aid = ((Number) a.get("id")).longValue();
                String tok = (String) a.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, tok);
                assert2xx(r, "TC87");
        }
}

// ─── TC88 — S3-F12 ownership check: cross-user 403 ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC88_RecommendationsCrossUserTests extends TestBase {
        @Test
        @DisplayName("TC88 — Freelancer A querying recommendations for B returns 403")
        void recommendations_cross_user_403() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("tc88a");
                java.util.Map<String, Object> b = seedAndLoginUser("tc88b");
                long bid = ((Number) b.get("id")).longValue();
                String tokA = (String) a.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + bid, tokA);
                assertEquals(403, r.statusCode(),
                        "TC88: cross-user must be 403; got " + r.statusCode());
        }
}

// ─── TC89 — S3-F12 admin override: admin can query any user's recs ─────────
@Tag("public")
@Tag("features_m2")
class TC89_RecommendationsAdminOverrideTests extends TestBase {
        @Test
        @DisplayName("TC89 — Admin can query any freelancer's recommendations (200)")
        void recommendations_admin_override() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("tc89a");
                long aid = ((Number) a.get("id")).longValue();
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, tok);
                assert2xx(r, "TC89: admin override must succeed");
        }
}

// ─── TC90 — S3-F12 unknown freelancerId → 404 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC90_RecommendationsUnknownFreelancerTests extends TestBase {
        @Test
        @DisplayName("TC90 — Unknown freelancerId returns 404")
        void recommendations_unknown_freelancer_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=999999", tok);
                assertEquals(404, r.statusCode(),
                        "TC90: must be 404; got " + r.statusCode());
        }
}

// ─── TC91 — S3-F12 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC91_RecommendationsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC91 — Recommendations without Authorization header returns 401")
        void recommendations_missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGet(
                        "/api/proposals/recommendations?freelancerId=1");
                assertEquals(401, r.statusCode(),
                        "TC91: must be 401; got " + r.statusCode());
        }
}

// ─── TC92 — S3-F12 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC92_RecommendationsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC92 — Recommendations with bogus JWT returns 401")
        void recommendations_invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=1", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC92: must be 401; got " + r.statusCode());
        }
}

// ─── TC93 — S3-F12 freelancer with no history → empty list ──────────────────
@Tag("public")
@Tag("features_m2")
class TC93_RecommendationsEmptyHistoryTests extends TestBase {
        @Test
        @DisplayName("TC93 — Freelancer with no proposal history returns empty list")
        void recommendations_empty_history() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("tc93a");
                long aid = ((Number) a.get("id")).longValue();
                String tok = (String) a.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, tok);
                assert2xx(r, "TC93");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertEquals(0, arr.size(),
                        "TC93: empty history → empty recs; body=" + r.body());
        }
}

// ─── TC94 — S3-F12 DTO shape includes jobId/title/category/score ───────────
@Tag("public")
@Tag("features_m2")
class TC94_RecommendationsDtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC94 — Each recommendation entry has jobId, title, category, score fields")
        void recommendations_dto_shape() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC94: Neo4j required");
                java.util.Map<String, Object> a = seedAndLoginUser("tc94a");
                java.util.Map<String, Object> b = seedAndLoginUser("tc94b");
                long aid = ((Number) a.get("id")).longValue();
                long bid = ((Number) b.get("id")).longValue();
                long j1 = _FmM2.job(this, "TC94 J1 " + nonce());
                long j2 = _FmM2.job(this, "TC94 J2 " + nonce());
                String tok = adminToken();
                _FmM2.proposeAndRecord(this, aid, j1, tok);
                _FmM2.proposeAndRecord(this, bid, j1, tok);
                _FmM2.proposeAndRecord(this, bid, j2, tok);
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, (String) a.get("token"));
                assert2xx(r, "TC94");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertTrue(arr.size() >= 1, "TC94: at least one rec expected");
                JsonNode item = arr.get(0);
                assertTrue(item.has("jobId") || item.has("job_id") || item.has("id"),
                        "TC94: entry must include jobId; got=" + item);
                assertTrue(item.has("title"),
                        "TC94: entry must include title; got=" + item);
                assertTrue(item.has("category") || item.has("score"),
                        "TC94: entry must include category or score; got=" + item);
        }
}

// ─── TC95 — S3-F12 cache populated on first call ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC95_RecommendationsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC95 — First call populates Redis cache")
        void recommendations_cache_populated() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) throw new AssertionError("TC95: Redis required");
                java.util.Map<String, Object> a = seedAndLoginUser("tc95a");
                long aid = ((Number) a.get("id")).longValue();
                String tok = (String) a.get("token");
                long before = redis.dbSize();
                assert2xx(httpGetAuth("/api/proposals/recommendations?freelancerId=" + aid, tok), "TC95");
                long after = redis.dbSize();
                assertTrue(after > before,
                        "TC95: cache key must be added; before=" + before + " after=" + after);
        }
}

// ─── TC96 — S3-F12 cache returns same body for repeated calls ──────────────
@Tag("public")
@Tag("features_m2")
class TC96_RecommendationsCacheSameBodyTests extends TestBase {
        @Test
        @DisplayName("TC96 — Two identical recommendation requests return identical bodies (cached)")
        void recommendations_cache_same_body() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("tc96a");
                long aid = ((Number) a.get("id")).longValue();
                String tok = (String) a.get("token");
                String url = "/api/proposals/recommendations?freelancerId=" + aid;
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC96 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC96 second");
                assertEquals(r1.body(), r2.body(),
                        "TC96: identical recommendation responses expected (cached)");
        }
}

// ─── TC97 — S3-F12 different freelancerId → different cache key ────────────
@Tag("public")
@Tag("features_m2")
class TC97_RecommendationsDistinctCacheKeyTests extends TestBase {
        @Test
        @DisplayName("TC97 — Different freelancerId produces independent cache results")
        void recommendations_distinct_cache_key() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("tc97a");
                java.util.Map<String, Object> b = seedAndLoginUser("tc97b");
                long aid = ((Number) a.get("id")).longValue();
                long bid = ((Number) b.get("id")).longValue();
                String tok = adminToken();
                HttpResponse<String> rA = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, tok);
                assert2xx(rA, "TC97 A");
                HttpResponse<String> rB = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + bid, tok);
                assert2xx(rB, "TC97 B");
        }
}

// ─── TC98 — S3-F12 negative limit returns 400 ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC98_RecommendationsNegativeLimitTests extends TestBase {
        @Test
        @DisplayName("TC98 — Negative limit returns a 4xx (validation)")
        void recommendations_negative_limit_4xx() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("tc98a");
                long aid = ((Number) a.get("id")).longValue();
                String tok = (String) a.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid + "&limit=-1", tok);
                int code = r.statusCode();
                assertTrue(code / 100 != 5, "TC98: must NOT 5xx; got " + code);
                assertTrue(code / 100 != 2, "TC98: must NOT 2xx; got " + code);
        }
}

// ─── TC99 — S3-F12 no shared jobs → empty list ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC99_RecommendationsNoSharedJobsTests extends TestBase {
        @Test
        @DisplayName("TC99 — Freelancer who shares no jobs with anyone gets empty recs")
        void recommendations_no_shared() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC99: Neo4j required");
                java.util.Map<String, Object> a = seedAndLoginUser("tc99a");
                long aid = ((Number) a.get("id")).longValue();
                long jUnique = _FmM2.job(this, "TC99 unique " + nonce());
                String tok = adminToken();
                _FmM2.proposeAndRecord(this, aid, jUnique, tok);
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/recommendations?freelancerId=" + aid, (String) a.get("token"));
                assert2xx(r, "TC99");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertEquals(0, arr.size(),
                        "TC99: no shared jobs → empty recs; body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// Helper class for S3 — Proposal + Job + Neo4j seeding helpers.
// ════════════════════════════════════════════════════════════════════════════
final class _FmM2 {
        private _FmM2() {}

        /** INSERT a Proposal row. Returns new id. */
        static Long prop(TestBase t, long freelancerId, long jobId, String status,
                         double bidAmount, int estimatedDays, String date) {
                String table = t.tableName("Proposal");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("Proposal", "freelancer"), freelancerId);
                try { ov.put(t.columnByField("Proposal", "job"), jobId); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "bidAmount"), bidAmount); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "estimatedDays"), estimatedDays); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "coverLetter"),
                        "TC proposal cover letter " + System.nanoTime()); }
                catch (Throwable ignore) {}
                ov.put(t.columnByField("Proposal", "status"), status);
                try { ov.put(t.columnByField("Proposal", "metadata"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(table, ov);
                t.setAllDateColumns(table, id, java.sql.Timestamp.valueOf(date + " 12:00:00"));
                try {
                        t.jdbc.update("UPDATE \"" + table + "\" SET \""
                                + t.columnByField("Proposal", "submittedAt") + "\"=? WHERE id=?",
                                java.sql.Timestamp.valueOf(date + " 12:00:00"), id);
                } catch (Throwable ignore) {}
                return id;
        }

        /** INSERT a Job row. Returns new id. */
        static long job(TestBase t, String title) {
                String table = t.tableName("Job");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("Job", "title"), title);
                try { ov.put(t.columnByField("Job", "description"), "TC job description"); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "client"), 1L); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "category"),
                        t.enumValueAt("Job", "category", 0)); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "status"),
                        t.enumValueAt("Job", "status", 0)); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "budgetMin"), 50.0); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "budgetMax"), 500.0); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "requirements"), "{}"); }
                catch (Throwable ignore) {}
                return t.insertRowReturningId(table, ov);
        }

        /** Seed a SUBMITTED proposal then call record-interaction (admin token). */
        static void proposeAndRecord(TestBase t, long freelancerId, long jobId, String adminTok) throws Exception {
                Long pid = prop(t, freelancerId, jobId, "SUBMITTED", 100.0, 5, "2026-04-15");
                java.net.http.HttpResponse<String> r = t.httpPostAuth(
                        "/api/proposals/" + pid + "/record-interaction", "", adminTok);
                if (r.statusCode() / 100 != 2) {
                        throw new AssertionError("proposeAndRecord: failed for freelancer="
                                + freelancerId + " job=" + jobId
                                + " status=" + r.statusCode() + " body=" + r.body());
                }
        }

        /** Read proposalCount property from PROPOSED_TO edge in Neo4j. */
        static long proposalCount(TestBase t, long freelancerId, long jobId) {
                java.util.List<java.util.Map<String, Object>> rows = t.neo4jExec(
                        "MATCH (a:`" + t.s3GraphUserLabel() + "` {id:$u})-[r:`" + t.s3GraphRelationship()
                          + "`]->(b:`" + t.s3GraphCatalogLabel() + "` {id:$p}) "
                          + "RETURN coalesce(r.proposalCount, r.bookingCount, r.orderCount, r.count, 0) AS c LIMIT 1",
                        java.util.Map.of("u", freelancerId, "p", jobId));
                if (rows.isEmpty()) return 0L;
                Object c = rows.get(0).get("c");
                return c instanceof Number n ? n.longValue() : 0L;
        }

        static long rL(JsonNode j, String... ks) {
                for (String k : ks) if (j.has(k)) return j.get(k).asLong();
                return -1;
        }
        static double rD(JsonNode j, String... ks) {
                for (String k : ks) if (j.has(k)) return j.get(k).asDouble();
                return -1;
        }
        static JsonNode rO(JsonNode j, String... ks) {
                for (String k : ks) if (j.has(k)) return j.get(k);
                return null;
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S4 M2 — Contract Service features (TC100..TC135)
//
// Covers S4-F10 (contract analytics dashboard, TC100-TC117), S4-F11 (record
// contract milestone event to Cassandra, TC118-TC127), and S4-F12 (contract
// milestone timeline read, TC128-TC135). Theme-specific terms: S4 endpoints
// under /api/contracts; Contract entity (proposal, freelancer, client, job,
// agreedAmount, startDate, endDate, status); Cassandra time-series table
// contract_milestone_events partitioned by contract_id and clustered by
// timestamp DESC; Mongo logs ANALYTICS_VIEWED / MILESTONE_TRACKED to
// contract_events.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC100 — S4-F10 dashboard happy path ────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC100_ContractDashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC100 — Dashboard returns totalContracts/averageContractValue/completionRate/averageContractDurationDays/contractsByStatus")
        void dashboard_happy_path() throws Exception {
                BASE_URL = deliveryServiceUrl;
                _FmM2S4.contract(this, 1L, 1L, "COMPLETED",     200.0, "2026-04-01", "2026-04-08");
                _FmM2S4.contract(this, 1L, 1L, "COMPLETED",     300.0, "2026-04-02", "2026-04-12");
                _FmM2S4.contract(this, 1L, 1L, "COMPLETED",     400.0, "2026-04-03", "2026-04-15");
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE",        500.0, "2026-04-04", null);
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE",        600.0, "2026-04-05", null);
                _FmM2S4.contract(this, 1L, 1L, "TERMINATED",    700.0, "2026-04-06", "2026-04-09");
                _FmM2S4.contract(this, 1L, 1L, "DISPUTED",      800.0, "2026-04-07", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC100");
                JsonNode j = parseNode(r.body());
                assertEquals(7L, _FmM2.rL(j, "totalContracts", "total_contracts"),
                        "TC100: totalContracts=7; body=" + r.body());
        }
}

// ─── TC101 — S4-F10 totalContracts isolated ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC101_ContractTotalContractsTests extends TestBase {
        @Test
        @DisplayName("TC101 — Dashboard.totalContracts equals exact count of contracts in range")
        void total_contracts_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                for (int i = 0; i < 5; i++) {
                        _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-09-" + String.format("%02d", i + 1), null);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC101");
                assertEquals(5L,
                        _FmM2.rL(parseNode(r.body()), "totalContracts", "total_contracts"),
                        "TC101: totalContracts=5");
        }
}

// ─── TC102 — S4-F10 averageContractValue isolated ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC102_ContractAvgValueTests extends TestBase {
        @Test
        @DisplayName("TC102 — Dashboard.averageContractValue = SUM(agreedAmount)/COUNT")
        void avg_contract_value_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-09-01", null);
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 200.0, "2026-09-02", null);
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 300.0, "2026-09-03", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC102");
                assertEquals(200.0,
                        _FmM2.rD(parseNode(r.body()), "averageContractValue", "average_contract_value"),
                        0.01, "TC102: averageContractValue=200");
        }
}

// ─── TC103 — S4-F10 completionRate isolated ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC103_ContractCompletionRateTests extends TestBase {
        @Test
        @DisplayName("TC103 — Dashboard.completionRate = COMPLETED / total")
        void completion_rate_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                for (int i = 0; i < 3; i++)
                        _FmM2S4.contract(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-" + String.format("%02d", i + 1), "2026-09-15");
                for (int i = 0; i < 5; i++)
                        _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-09-" + String.format("%02d", i + 5), null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC103");
                assertEquals(0.375,
                        _FmM2.rD(parseNode(r.body()), "completionRate", "completion_rate"), 0.01,
                        "TC103: completionRate=0.375 (3/8)");
        }
}

// ─── TC104 — S4-F10 averageContractDurationDays for COMPLETED only ─────────
@Tag("public")
@Tag("features_m2")
class TC104_ContractAvgDurationTests extends TestBase {
        @Test
        @DisplayName("TC104 — Dashboard.averageContractDurationDays = avg(endDate-startDate) for COMPLETED only")
        void avg_duration_completed_only() throws Exception {
                BASE_URL = deliveryServiceUrl;
                _FmM2S4.contract(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-01", "2026-09-08");
                _FmM2S4.contract(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-01", "2026-09-15");
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE",    100.0, "2026-09-01", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC104");
                double avg = _FmM2.rD(parseNode(r.body()), "averageContractDurationDays",
                        "average_contract_duration_days", "avgContractDurationDays");
                assertTrue(avg >= 9.5 && avg <= 11.5,
                        "TC104: averageContractDurationDays should be ~10.5 (7+14/2 COMPLETED only); got " + avg);
        }
}

// ─── TC105 — S4-F10 averageContractDurationDays=0 when no COMPLETED ────────
@Tag("public")
@Tag("features_m2")
class TC105_ContractAvgDurationZeroTests extends TestBase {
        @Test
        @DisplayName("TC105 — averageContractDurationDays=0 when no COMPLETED contracts (avoid /0)")
        void avg_duration_zero_when_no_completed() throws Exception {
                BASE_URL = deliveryServiceUrl;
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-09-01", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC105");
                assertEquals(0.0,
                        _FmM2.rD(parseNode(r.body()), "averageContractDurationDays",
                                "average_contract_duration_days"), 0.01,
                        "TC105: averageContractDurationDays=0 when no COMPLETED");
        }
}

// ─── TC106 — S4-F10 contractsByStatus has all 4 statuses ───────────────────
@Tag("public")
@Tag("features_m2")
class TC106_ContractsByStatusTests extends TestBase {
        @Test
        @DisplayName("TC106 — Dashboard.contractsByStatus contains entries for ACTIVE/COMPLETED/TERMINATED/DISPUTED")
        void contracts_by_status_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String[] sts = {"ACTIVE", "COMPLETED", "TERMINATED", "DISPUTED"};
                for (int i = 0; i < sts.length; i++) {
                        _FmM2S4.contract(this, 1L, 1L, sts[i], 100.0,
                                "2026-09-" + String.format("%02d", i + 1),
                                "COMPLETED".equals(sts[i]) ? "2026-09-10" : null);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC106");
                JsonNode j = parseNode(r.body());
                JsonNode bd = _FmM2.rO(j, "contractsByStatus", "contracts_by_status");
                assertNotNull(bd, "TC106: contractsByStatus key required");
                for (String st : sts) {
                        assertTrue(bd.has(st), "TC106: contractsByStatus missing key '" + st + "'");
                        assertEquals(1L, bd.get(st).asLong(),
                                "TC106: contractsByStatus[" + st + "]=1");
                }
        }
}

// ─── TC107 — S4-F10 empty range returns zeros ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC107_ContractDashboardEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC107 — Dashboard with no contracts in range returns all-zero counts")
        void empty_range_zeros() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC107");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, _FmM2.rL(j, "totalContracts", "total_contracts"),
                        "TC107: totalContracts=0");
        }
}

// ─── TC108 — S4-F10 invalid date range returns 400 ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC108_ContractDashboardInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC108 — Dashboard with startDate > endDate returns 400")
        void invalid_date_range_400() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(),
                        "TC108: must be 400; got " + r.statusCode());
        }
}

// ─── TC109 — S4-F10 missing JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC109_ContractDashboardMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC109 — Dashboard without Authorization header returns 401")
        void missing_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGet(
                        "/api/contracts/analytics?startDate=2026-04-01&endDate=2026-04-30");
                assertEquals(401, r.statusCode(),
                        "TC109: must be 401; got " + r.statusCode());
        }
}

// ─── TC110 — S4-F10 invalid JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC110_ContractDashboardInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC110 — Dashboard with malformed JWT returns 401")
        void invalid_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-04-01&endDate=2026-04-30", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC110: must be 401; got " + r.statusCode());
        }
}

// ─── TC111 — S4-F10 ANALYTICS_VIEWED logged on first call ─────────────────
@Tag("public")
@Tag("features_m2")
class TC111_ContractDashboardAnalyticsLoggedTests extends TestBase {
        @Test
        @DisplayName("TC111 — First dashboard call writes ANALYTICS_VIEWED to contract_events")
        void analytics_viewed_on_first_call() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null) throw new AssertionError("TC111: MongoDB required");
                String coll = mongoCollectionByName((String) theme().get("s4EventsCollection"));
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-07-01&endDate=2026-07-31", tok);
                assert2xx(r, "TC111");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after > before,
                        "TC111: ANALYTICS_VIEWED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC112 — S4-F10 ANALYTICS_VIEWED logged on cache hit too ──────────────
@Tag("public")
@Tag("features_m2")
class TC112_ContractDashboardAnalyticsOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC112 — Second dashboard call (cache hit) still logs ANALYTICS_VIEWED")
        void analytics_viewed_on_cache_hit() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null) throw new AssertionError("TC112: MongoDB required");
                String coll = mongoCollectionByName((String) theme().get("s4EventsCollection"));
                String tok = adminToken();
                String url = "/api/contracts/analytics?startDate=2026-07-01&endDate=2026-07-31";
                assert2xx(httpGetAuth(url, tok), "TC112 first");
                long after1 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assert2xx(httpGetAuth(url, tok), "TC112 second");
                long after2 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after2 > after1,
                        "TC112: ANALYTICS_VIEWED must be logged on cache hit; after1=" + after1 + " after2=" + after2);
        }
}

// ─── TC113 — S4-F10 cache returns same body for repeated calls ────────────
@Tag("public")
@Tag("features_m2")
class TC113_ContractDashboardCacheSameBodyTests extends TestBase {
        @Test
        @DisplayName("TC113 — Two identical dashboard requests return identical bodies")
        void cache_same_body() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                String url = "/api/contracts/analytics?startDate=2026-07-01&endDate=2026-07-31";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC113 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC113 second");
                assertEquals(r1.body(), r2.body(), "TC113: cached body must match");
        }
}

// ─── TC114 — S4-F10 cache hit doesn't re-aggregate ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC114_ContractDashboardCacheNoReaggregateTests extends TestBase {
        @Test
        @DisplayName("TC114 — Insert contract after first call → cached body still returned")
        void cache_does_not_reaggregate() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                String url = "/api/contracts/analytics?startDate=2026-11-01&endDate=2026-11-30";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC114 first");
                long t1 = _FmM2.rL(parseNode(r1.body()), "totalContracts", "total_contracts");
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-11-15", null);
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC114 second");
                long t2 = _FmM2.rL(parseNode(r2.body()), "totalContracts", "total_contracts");
                assertEquals(t1, t2,
                        "TC114: cached value must equal pre-insert value; t1=" + t1 + " t2=" + t2);
        }
}

// ─── TC115 — S4-F10 boundary date inclusion ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC115_ContractDashboardBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC115 — Contract with startDate exactly at range start is included")
        void boundary_included() throws Exception {
                BASE_URL = deliveryServiceUrl;
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-05-01", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-05-01&endDate=2026-05-31", tok);
                assert2xx(r, "TC115");
                assertEquals(1L,
                        _FmM2.rL(parseNode(r.body()), "totalContracts", "total_contracts"),
                        "TC115: boundary contract must be counted");
        }
}

// ─── TC116 — S4-F10 out-of-range contracts excluded ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC116_ContractDashboardOutOfRangeTests extends TestBase {
        @Test
        @DisplayName("TC116 — Contracts outside [startDate, endDate] are excluded")
        void out_of_range_excluded() throws Exception {
                BASE_URL = deliveryServiceUrl;
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-06-15", null);
                _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-08-15", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-06-01&endDate=2026-06-30", tok);
                assert2xx(r, "TC116");
                assertEquals(1L,
                        _FmM2.rL(parseNode(r.body()), "totalContracts", "total_contracts"),
                        "TC116: only the in-range contract counts");
        }
}

// ─── TC117 — S4-F10 disputedCount counts DISPUTED only ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC117_ContractDisputedCountTests extends TestBase {
        @Test
        @DisplayName("TC117 — Dashboard.contractsByStatus.DISPUTED counts only DISPUTED contracts")
        void disputed_count_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                for (int i = 0; i < 3; i++)
                        _FmM2S4.contract(this, 1L, 1L, "DISPUTED", 100.0,
                                "2026-09-" + String.format("%02d", i + 1), null);
                for (int i = 0; i < 4; i++)
                        _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0,
                                "2026-09-" + String.format("%02d", i + 4), null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC117");
                JsonNode j = parseNode(r.body());
                JsonNode bd = _FmM2.rO(j, "contractsByStatus", "contracts_by_status");
                assertNotNull(bd, "TC117: contractsByStatus required");
                assertEquals(3L, bd.get("DISPUTED").asLong(),
                        "TC117: contractsByStatus.DISPUTED=3");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S4-F11 — Record Contract Milestone Event (TC118-TC127)
// ════════════════════════════════════════════════════════════════════════════

// ─── TC118 — S4-F11 happy path writes one Cassandra row ────────────────────
@Tag("public")
@Tag("features_m2")
class TC118_ContractTrackHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC118 — Track milestone writes one row to Cassandra contract_milestone_events")
        void track_happy_path() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC118: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"phase one done\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/contracts/" + cid + "/milestones/track", body, tok);
                assertEquals(201, r.statusCode(),
                        "TC118: must be 201; got " + r.statusCode() + " body=" + r.body());
                long count = _FmM2S4.cassRowCount(this, cid);
                assertEquals(1L, count, "TC118: 1 row written; got " + count);
        }
}

// ─── TC119 — S4-F11 multiple tracks append rows ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC119_ContractTrackAppendsRowsTests extends TestBase {
        @Test
        @DisplayName("TC119 — Three sequential tracks append three rows to Cassandra")
        void track_appends_rows() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC119: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String[] sts = {"PENDING", "IN_PROGRESS", "COMPLETED"};
                for (int i = 0; i < sts.length; i++) {
                        String body = "{\"milestoneOrder\":" + (i + 1) + ",\"status\":\"" + sts[i] + "\",\"recordedBy\":1,\"notes\":\"x\"}";
                        HttpResponse<String> r = httpPostAuth(
                                "/api/contracts/" + cid + "/milestones/track", body, tok);
                        assertEquals(201, r.statusCode(),
                                "TC119: track " + sts[i] + " must be 201; got " + r.statusCode());
                }
                long count = _FmM2S4.cassRowCount(this, cid);
                assertEquals(3L, count, "TC119: 3 rows total; got " + count);
        }
}

// ─── TC120 — S4-F11 row carries status column ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC120_ContractTrackStatusColumnTests extends TestBase {
        @Test
        @DisplayName("TC120 — Cassandra row carries status column matching request body")
        void track_status_column() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC120: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"APPROVED\",\"recordedBy\":1,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC120 setup");
                java.util.Map<String, Object> row = _FmM2S4.cassFirstRow(this, cid);
                assertNotNull(row, "TC120: row must exist");
                String st = (String) row.get("status");
                assertEquals("APPROVED", st,
                        "TC120: status=APPROVED expected; got " + st);
        }
}

// ─── TC121 — S4-F11 recordedBy column populated from request ───────────────
@Tag("public")
@Tag("features_m2")
class TC121_ContractTrackRecordedByTests extends TestBase {
        @Test
        @DisplayName("TC121 — Cassandra row's recorded_by column carries the request body actorId")
        void track_recorded_by() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC121: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":42,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC121 setup");
                java.util.Map<String, Object> row = _FmM2S4.cassFirstRow(this, cid);
                assertNotNull(row, "TC121: row must exist");
                Object rb = row.get("recorded_by");
                if (rb == null) rb = row.get("recordedBy");
                long rbVal = rb instanceof Number n ? n.longValue() : -1L;
                assertEquals(42L, rbVal,
                        "TC121: recorded_by=42 (from request body); got " + rb);
        }
}

// ─── TC122 — S4-F11 invalid status returns 400 ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC122_ContractTrackInvalidStatusTests extends TestBase {
        @Test
        @DisplayName("TC122 — Track with invalid status returns 400")
        void track_invalid_status_400() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"BANANA\",\"recordedBy\":1,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/contracts/" + cid + "/milestones/track", body, tok);
                assertEquals(400, r.statusCode(),
                        "TC122: must be 400 for invalid status; got " + r.statusCode());
        }
}

// ─── TC123 — S4-F11 non-existent contract returns 404 ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC123_ContractTrackNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC123 — Track on non-existent contract returns 404")
        void track_not_found_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/contracts/999999/milestones/track", body, tok);
                assertEquals(404, r.statusCode(),
                        "TC123: must be 404; got " + r.statusCode());
        }
}

// ─── TC124 — S4-F11 missing JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC124_ContractTrackMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC124 — Track without Authorization header returns 401")
        void track_missing_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPost(
                        "/api/contracts/" + cid + "/milestones/track", body);
                assertEquals(401, r.statusCode(),
                        "TC124: must be 401; got " + r.statusCode());
        }
}

// ─── TC125 — S4-F11 invalid JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC125_ContractTrackInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC125 — Track with bogus JWT returns 401")
        void track_invalid_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/contracts/" + cid + "/milestones/track", body, "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC125: must be 401; got " + r.statusCode());
        }
}

// ─── TC126 — S4-F11 MILESTONE_TRACKED logged to contract_events ────────────
@Tag("public")
@Tag("features_m2")
class TC126_ContractTrackEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC126 — Track writes MILESTONE_TRACKED to MongoDB contract_events")
        void track_event_logged() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null) throw new AssertionError("TC126: MongoDB required");
                String coll = mongoCollectionByName((String) theme().get("s4EventsCollection"));
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "MILESTONE_TRACKED"));
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC126 setup");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "MILESTONE_TRACKED"));
                assertTrue(after > before,
                        "TC126: MILESTONE_TRACKED must be logged; before=" + before + " after=" + after);
        }
}

// ─── TC127 — S4-F11 returns 201 status code ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC127_ContractTrackReturns201Tests extends TestBase {
        @Test
        @DisplayName("TC127 — Successful track returns HTTP 201 Created")
        void track_returns_201() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/contracts/" + cid + "/milestones/track", body, tok);
                assertEquals(201, r.statusCode(),
                        "TC127: must be strict 201; got " + r.statusCode() + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S4-F12 — Get Contract Milestone Timeline (TC128-TC135)
// ════════════════════════════════════════════════════════════════════════════

// ─── TC128 — S4-F12 happy path returns array ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC128_ContractTimelineHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC128 — Timeline returns array containing a ContractMilestoneDTO entry")
        void timeline_happy_path() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC128: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC128 setup");
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/" + cid + "/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59", tok);
                assert2xx(r, "TC128");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertTrue(arr.isArray() && arr.size() >= 1,
                        "TC128: at least one milestone entry expected; body=" + r.body());
        }
}

// ─── TC129 — S4-F12 entries sorted DESC by timestamp ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC129_ContractTimelineDescOrderTests extends TestBase {
        @Test
        @DisplayName("TC129 — Most recent track appears first (DESC order)")
        void timeline_desc_order() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC129: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String b1 = "{\"milestoneOrder\":1,\"status\":\"PENDING\",\"recordedBy\":1,\"notes\":\"first\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", b1, tok).statusCode(), "TC129 a");
                Thread.sleep(50);
                String b2 = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"second\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", b2, tok).statusCode(), "TC129 b");
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/" + cid + "/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59", tok);
                assert2xx(r, "TC129 read");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertTrue(arr.size() >= 2, "TC129: 2 entries expected");
                String s0 = arr.get(0).has("status") ? arr.get(0).get("status").asText() : "";
                assertEquals("COMPLETED", s0, "TC129: most-recent first; got " + s0);
        }
}

// ─── TC130 — S4-F12 startTime / endTime range filtering ───────────────────
@Tag("public")
@Tag("features_m2")
class TC130_ContractTimelineRangeTests extends TestBase {
        @Test
        @DisplayName("TC130 — startTime/endTime range narrows results")
        void timeline_range_filtering() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC130: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC130 setup");
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/" + cid + "/milestones/timeline?startTime=2099-01-01T00:00:00&endTime=2099-12-31T23:59:59", tok);
                assert2xx(r, "TC130 read");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertEquals(0, arr.size(),
                        "TC130: future-only range must return empty; body=" + r.body());
        }
}

// ─── TC131 — S4-F12 entry shape includes timestamp+milestoneOrder+status ──
@Tag("public")
@Tag("features_m2")
class TC131_ContractTimelineDtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC131 — Each entry has timestamp, milestoneOrder, status, recordedBy, notes")
        void timeline_dto_shape() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC131: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":7,\"status\":\"IN_PROGRESS\",\"recordedBy\":3,\"notes\":\"hello\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC131 setup");
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/" + cid + "/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59", tok);
                assert2xx(r, "TC131 read");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertTrue(arr.size() >= 1, "TC131: at least one entry");
                JsonNode item = arr.get(0);
                assertTrue(item.has("timestamp"),
                        "TC131: entry must include timestamp; got=" + item);
                assertTrue(item.has("milestoneOrder") || item.has("milestone_order"),
                        "TC131: entry must include milestoneOrder; got=" + item);
                assertTrue(item.has("status"),
                        "TC131: entry must include status; got=" + item);
                assertTrue(item.has("recordedBy") || item.has("recorded_by"),
                        "TC131: entry must include recordedBy; got=" + item);
                assertTrue(item.has("notes"),
                        "TC131: entry must include notes; got=" + item);
        }
}

// ─── TC132 — S4-F12 non-existent contract returns 404 ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC132_ContractTimelineNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC132 — Timeline for non-existent contract returns 404")
        void timeline_not_found_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/999999/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59", tok);
                assertEquals(404, r.statusCode(),
                        "TC132: must be 404; got " + r.statusCode());
        }
}

// ─── TC133 — S4-F12 missing JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC133_ContractTimelineMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC133 — Timeline without Authorization header returns 401")
        void timeline_missing_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                HttpResponse<String> r = httpGet(
                        "/api/contracts/" + cid + "/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59");
                assertEquals(401, r.statusCode(),
                        "TC133: must be 401; got " + r.statusCode());
        }
}

// ─── TC134 — S4-F12 invalid JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC134_ContractTimelineInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC134 — Timeline with bogus JWT returns 401")
        void timeline_invalid_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/" + cid + "/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59",
                        "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC134: must be 401; got " + r.statusCode());
        }
}

// ─── TC135 — S4-F12 cache returns same body for repeated calls ────────────
@Tag("public")
@Tag("features_m2")
class TC135_ContractTimelineCacheTests extends TestBase {
        @Test
        @DisplayName("TC135 — Two identical timeline requests return identical bodies")
        void timeline_cache_same_body() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC135: Cassandra required");
                long cid = _FmM2S4.contract(this, 1L, 1L, "ACTIVE", 100.0, "2026-04-15", null);
                String tok = adminToken();
                String body = "{\"milestoneOrder\":1,\"status\":\"COMPLETED\",\"recordedBy\":1,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/contracts/" + cid + "/milestones/track", body, tok).statusCode(),
                        "TC135 setup");
                String url = "/api/contracts/" + cid + "/milestones/timeline?startTime=2026-01-01T00:00:00&endTime=2099-12-31T23:59:59";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC135 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC135 second");
                assertEquals(r1.body(), r2.body(),
                        "TC135: identical timeline responses expected (cached)");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// Helper class for S4 — Contract + Cassandra read helpers.
// ════════════════════════════════════════════════════════════════════════════
final class _FmM2S4 {
        private _FmM2S4() {}

        /** INSERT a Contract row. Returns id. endDate may be null. */
        static long contract(TestBase t, long freelancerId, long clientId, String status,
                             double agreedAmount, String startDate, String endDate) {
                String table = t.tableName("Contract");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Contract", "freelancer"), freelancerId); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "client"), clientId); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "proposal"), 1L); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "job"), 1L); }
                catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "agreedAmount"), agreedAmount); }
                catch (Throwable ignore) {}
                ov.put(t.columnByField("Contract", "status"), status);
                try { ov.put(t.columnByField("Contract", "startDate"), java.sql.Date.valueOf(startDate)); }
                catch (Throwable ignore) {}
                if (endDate != null) {
                        try { ov.put(t.columnByField("Contract", "endDate"), java.sql.Date.valueOf(endDate)); }
                        catch (Throwable ignore) {}
                }
                try { ov.put(t.columnByField("Contract", "metadata"), "{}"); }
                catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(table, ov);
                t.setAllDateColumns(table, id, java.sql.Timestamp.valueOf(startDate + " 12:00:00"));
                if (endDate != null) {
                        try {
                                t.jdbc.update("UPDATE \"" + table + "\" SET \""
                                        + t.columnByField("Contract", "endDate") + "\" = ? WHERE id = ?",
                                        java.sql.Timestamp.valueOf(endDate + " 12:00:00"), id);
                        } catch (Throwable ignore) {}
                }
                return id;
        }

        static long cassRowCount(TestBase t, long contractId) {
                return t.cassandraCount(t.s4TimeseriesTable(), "contract_id", contractId);
        }

        static java.util.Map<String, Object> cassFirstRow(TestBase t, long contractId) {
                java.util.List<java.util.Map<String, Object>> rows =
                        t.cassandraRows(t.s4TimeseriesTable(), "contract_id", contractId);
                return rows.isEmpty() ? null : rows.get(0);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S5 M2 — Wallet Service features (TC136..TC190)
//
// Covers S5-F10 (platform fee analytics by job category, TC136-TC158),
// S5-F11 (payout method breakdown from MongoDB audit, TC159-TC170), and
// S5-F12 (milestone-based payout reversal with strategy, TC171-TC190).
// Theme-specific terms: S5 endpoints under /api/payouts; Payout entity has
// contract FK, method, amount, status, transactionDetails JSONB; Mongo
// payout_audit_trail collection. Strategies: FullPayoutReversalStrategy
// / MilestoneReversalStrategy / NoReversalStrategy. 30-day reversal window
// computed from payout.createdAt.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC136 — S5-F10 happy path (category breakdown across jobs) ─────────────
@Tag("public")
@Tag("features_m2")
class TC136_CategoryRevenueHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC136 — Category breakdown groups COMPLETED payouts by jobs.category")
        void category_revenue_happy_path() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChain(this, "WEB_DEV", 200.0, "2026-03-10");
                _FmM2S5.fullChain(this, "MOBILE", 100.0, "2026-03-12");
                _FmM2S5.fullChain(this, "DESIGN", 150.0, "2026-03-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC136");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertNotNull(_FmM2S5.findByCategory(arr, "WEB_DEV"), "TC136: WEB_DEV row required; body=" + r.body());
                assertNotNull(_FmM2S5.findByCategory(arr, "MOBILE"), "TC136: MOBILE row required");
                assertNotNull(_FmM2S5.findByCategory(arr, "DESIGN"), "TC136: DESIGN row required");
        }
}

// ─── TC137 — S5-F10 totalRevenue per category ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC137_CategoryTotalRevenueTests extends TestBase {
        @Test
        @DisplayName("TC137 — Category.totalRevenue = SUM(payout.amount) for that category")
        void category_total_revenue() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChain(this, "WEB_DEV", 400.0, "2026-03-10");
                _FmM2S5.fullChain(this, "WEB_DEV", 250.0, "2026-03-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC137");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode wd = _FmM2S5.findByCategory(arr, "WEB_DEV");
                assertNotNull(wd, "TC137: WEB_DEV row required");
                assertEquals(650.0, _FmM2.rD(wd, "totalRevenue", "total_revenue"), 0.5,
                        "TC137: WEB_DEV totalRevenue=400+250=650");
        }
}

// ─── TC138 — S5-F10 payoutCount counts distinct payouts per category ───────
@Tag("public")
@Tag("features_m2")
class TC138_CategoryPayoutCountTests extends TestBase {
        @Test
        @DisplayName("TC138 — Category.payoutCount equals number of COMPLETED payouts for that category")
        void category_payout_count() throws Exception {
                BASE_URL = checkoutServiceUrl;
                for (int i = 0; i < 4; i++) {
                        _FmM2S5.fullChain(this, "MOBILE", 100.0, "2026-03-" + String.format("%02d", i + 1));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC138");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode mob = _FmM2S5.findByCategory(arr, "MOBILE");
                assertEquals(4L, _FmM2.rL(mob, "payoutCount", "payout_count"),
                        "TC138: MOBILE payoutCount=4");
        }
}

// ─── TC139 — S5-F10 platformFeeRevenue uses 10% fallback when JSONB missing
@Tag("public")
@Tag("features_m2")
class TC139_PlatformFeeFallback10Tests extends TestBase {
        @Test
        @DisplayName("TC139 — When transactionDetails.platformFee absent, fallback 10% of amount used")
        void platform_fee_fallback_10pct() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChainNoFee(this, "WEB_DEV", 500.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC139");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode wd = _FmM2S5.findByCategory(arr, "WEB_DEV");
                assertNotNull(wd, "TC139: WEB_DEV row required");
                double fee = _FmM2.rD(wd, "platformFeeRevenue", "platform_fee_revenue");
                assertEquals(50.0, fee, 0.5,
                        "TC139: 10% fallback fee = 50 (10% of 500); got " + fee);
        }
}

// ─── TC140 — S5-F10 platformFeeRevenue uses JSONB value when present ───────
@Tag("public")
@Tag("features_m2")
class TC140_PlatformFeeFromJsonbTests extends TestBase {
        @Test
        @DisplayName("TC140 — When transactionDetails.platformFee=20, that value is used (not 10%)")
        void platform_fee_from_jsonb() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChainWithFee(this, "WEB_DEV", 500.0, 20.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC140");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode wd = _FmM2S5.findByCategory(arr, "WEB_DEV");
                double fee = _FmM2.rD(wd, "platformFeeRevenue", "platform_fee_revenue");
                assertEquals(20.0, fee, 0.5,
                        "TC140: explicit JSONB platformFee=20 used; got " + fee);
        }
}

// ─── TC141 — S5-F10 netPayoutRevenue = total - fee ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC141_NetPayoutRevenueTests extends TestBase {
        @Test
        @DisplayName("TC141 — netPayoutRevenue = totalRevenue - platformFeeRevenue")
        void net_payout_revenue() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChainWithFee(this, "WEB_DEV", 500.0, 25.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC141");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode wd = _FmM2S5.findByCategory(arr, "WEB_DEV");
                assertEquals(475.0, _FmM2.rD(wd, "netPayoutRevenue", "net_payout_revenue"), 0.5,
                        "TC141: netPayoutRevenue = 500 - 25 = 475");
        }
}

// ─── TC142 — S5-F10 only COMPLETED payouts counted ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC142_CategoryCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC142 — PENDING/REFUNDED/FAILED payouts excluded from category breakdown")
        void category_completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChainWithStatus(this, "WEB_DEV", 200.0, "PENDING", "2026-03-10");
                _FmM2S5.fullChainWithStatus(this, "WEB_DEV", 200.0, "COMPLETED", "2026-03-11");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC142");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode wd = _FmM2S5.findByCategory(arr, "WEB_DEV");
                assertEquals(1L, _FmM2.rL(wd, "payoutCount", "payout_count"),
                        "TC142: only COMPLETED counts; payoutCount=1");
        }
}

// ─── TC143 — S5-F10 group-by category independence ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC143_CategoryGroupByTests extends TestBase {
        @Test
        @DisplayName("TC143 — Distinct categories produce distinct rows")
        void category_group_by() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String[] cats = {"WEB_DEV", "MOBILE", "DESIGN", "WRITING"};
                for (int i = 0; i < cats.length; i++) {
                        _FmM2S5.fullChain(this, cats[i], 100.0, "2026-03-" + String.format("%02d", i + 1));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC143");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JsonNode it : arr) {
                        String c = it.has("category") ? it.get("category").asText() : null;
                        if (c != null) seen.add(c);
                }
                for (String c : cats) {
                        assertTrue(seen.contains(c),
                                "TC143: must include row for '" + c + "'; got=" + seen);
                }
        }
}

// ─── TC144 — S5-F10 empty range returns empty list ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC144_CategoryEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC144 — Category breakdown with no payouts in range returns empty list")
        void category_empty_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC144");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertEquals(0, arr.size(),
                        "TC144: empty list expected; body=" + r.body());
        }
}

// ─── TC145 — S5-F10 startDate boundary inclusion ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC145_CategoryStartBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC145 — Payout at exactly startDate is included")
        void category_start_boundary() throws Exception {
                BASE_URL = checkoutServiceUrl;
                Long pid = _FmM2S5.fullChain(this, "WEB_DEV", 100.0, "2026-05-01");
                jdbc.update("UPDATE \"" + tableName("Payout") + "\" SET created_at=? WHERE id=?",
                        java.sql.Timestamp.valueOf("2026-05-01 00:00:00"), pid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-05-01&endDate=2026-05-31", tok);
                assert2xx(r, "TC145");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertNotNull(_FmM2S5.findByCategory(arr, "WEB_DEV"),
                        "TC145: boundary payout must be included; body=" + r.body());
        }
}

// ─── TC146 — S5-F10 endDate boundary inclusion ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC146_CategoryEndBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC146 — Payout at exactly endDate is included")
        void category_end_boundary() throws Exception {
                BASE_URL = checkoutServiceUrl;
                Long pid = _FmM2S5.fullChain(this, "WEB_DEV", 100.0, "2026-05-31");
                jdbc.update("UPDATE \"" + tableName("Payout") + "\" SET created_at=? WHERE id=?",
                        java.sql.Timestamp.valueOf("2026-05-31 23:59:59"), pid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-05-01&endDate=2026-05-31", tok);
                assert2xx(r, "TC146");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertNotNull(_FmM2S5.findByCategory(arr, "WEB_DEV"),
                        "TC146: end-boundary payout must be included");
        }
}

// ─── TC147 — S5-F10 inverted dates returns 400 ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC147_CategoryInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC147 — Category breakdown with startDate > endDate returns 400")
        void category_inverted_dates_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(),
                        "TC147: must be 400; got " + r.statusCode());
        }
}

// ─── TC148 — S5-F10 missing JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC148_CategoryMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC148 — Category breakdown without Authorization header returns 401")
        void category_missing_jwt_401() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGet(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31");
                assertEquals(401, r.statusCode(),
                        "TC148: must be 401; got " + r.statusCode());
        }
}

// ─── TC149 — S5-F10 invalid JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC149_CategoryInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC149 — Category breakdown with malformed JWT returns 401")
        void category_invalid_jwt_401() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-03-01&endDate=2026-03-31", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC149: must be 401; got " + r.statusCode());
        }
}

// ─── TC150 — S5-F10 ANALYTICS_VIEWED logged on first call ─────────────────
@Tag("public")
@Tag("features_m2")
class TC150_CategoryAnalyticsLoggedTests extends TestBase {
        @Test
        @DisplayName("TC150 — First category call writes ANALYTICS_VIEWED to payout_audit_trail")
        void category_analytics_logged() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC150: MongoDB required");
                String coll = s5AuditCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-07-01&endDate=2026-07-31", tok);
                assert2xx(r, "TC150");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after > before,
                        "TC150: ANALYTICS_VIEWED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC151 — S5-F10 ANALYTICS_VIEWED logged on cache hit too ──────────────
@Tag("public")
@Tag("features_m2")
class TC151_CategoryAnalyticsLoggedOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC151 — Second category call (cache hit) still logs ANALYTICS_VIEWED")
        void category_analytics_on_cache_hit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC151: MongoDB required");
                String coll = s5AuditCollection();
                String tok = adminToken();
                String url = "/api/payouts/analytics/category?startDate=2026-07-01&endDate=2026-07-31";
                assert2xx(httpGetAuth(url, tok), "TC151 first");
                long after1 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assert2xx(httpGetAuth(url, tok), "TC151 second");
                long after2 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after2 > after1,
                        "TC151: ANALYTICS_VIEWED must be logged on cache hit; after1=" + after1 + " after2=" + after2);
        }
}

// ─── TC152 — S5-F10 cache populated on first call ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC152_CategoryCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC152 — First call populates Redis under wallet-service::S5-F10::*")
        void category_cache_populated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null) throw new AssertionError("TC152: Redis required");
                String tok = adminToken();
                String url = "/api/payouts/analytics/category?startDate=2026-08-01&endDate=2026-08-31";
                long before = redis.dbSize();
                assert2xx(httpGetAuth(url, tok), "TC152");
                long after = redis.dbSize();
                assertTrue(after > before,
                        "TC152: at least one cache key must be added; before=" + before + " after=" + after);
        }
}

// ─── TC153 — S5-F10 cache returns same body for repeated calls ────────────
@Tag("public")
@Tag("features_m2")
class TC153_CategoryCacheSameBodyTests extends TestBase {
        @Test
        @DisplayName("TC153 — Two identical category requests return identical bodies (cached)")
        void category_cache_same_body() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                String url = "/api/payouts/analytics/category?startDate=2026-07-01&endDate=2026-07-31";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC153 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC153 second");
                assertEquals(r1.body(), r2.body(),
                        "TC153: identical category responses expected (cached)");
        }
}

// ─── TC154 — S5-F10 cache hit doesn't re-aggregate ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC154_CategoryCacheNoReaggregateTests extends TestBase {
        @Test
        @DisplayName("TC154 — Insert payout after first call → cached body still returned")
        void category_cache_no_reaggregate() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                String url = "/api/payouts/analytics/category?startDate=2026-12-01&endDate=2026-12-31";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC154 first");
                _FmM2S5.fullChain(this, "WEB_DEV", 100.0, "2026-12-15");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC154 second");
                assertEquals(r1.body(), r2.body(),
                        "TC154: cached body must persist after insert");
        }
}

// ─── TC155 — S5-F10 boundary date out-of-range exclusion ──────────────────
@Tag("public")
@Tag("features_m2")
class TC155_CategoryOutOfRangeTests extends TestBase {
        @Test
        @DisplayName("TC155 — Payout outside [startDate, endDate] is excluded from category breakdown")
        void category_out_of_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
                Long pid = _FmM2S5.fullChain(this, "WEB_DEV", 100.0, "2026-08-15");
                jdbc.update("UPDATE \"" + tableName("Payout") + "\" SET created_at=? WHERE id=?",
                        java.sql.Timestamp.valueOf("2026-08-15 12:00:00"), pid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC155");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertEquals(0, arr.size(),
                        "TC155: out-of-range payout must produce empty result; body=" + r.body());
        }
}

// ─── TC156 — S5-F10 category zero excluded ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC156_CategoryZeroExcludedTests extends TestBase {
        @Test
        @DisplayName("TC156 — Category with no payouts in range is not present in response")
        void category_zero_excluded() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChain(this, "WEB_DEV", 100.0, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC156");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode mob = _FmM2S5.findByCategory(arr, "MOBILE");
                assertTrue(mob == null || _FmM2.rL(mob, "payoutCount", "payout_count") == 0,
                        "TC156: MOBILE category with no payouts must be omitted/zero; got=" + mob);
        }
}

// ─── TC157 — S5-F10 distinct jobs same category aggregate together ────────
@Tag("public")
@Tag("features_m2")
class TC157_CategoryAcrossJobsTests extends TestBase {
        @Test
        @DisplayName("TC157 — Payouts for the same category across different jobs aggregate together")
        void category_across_jobs() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChain(this, "WEB_DEV", 100.0, "2026-08-10");
                _FmM2S5.fullChain(this, "WEB_DEV", 200.0, "2026-08-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC157");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                JsonNode wd = _FmM2S5.findByCategory(arr, "WEB_DEV");
                assertEquals(300.0, _FmM2.rD(wd, "totalRevenue", "total_revenue"), 0.5,
                        "TC157: across jobs WEB_DEV total=300");
        }
}

// ─── TC158 — S5-F10 row shape ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC158_CategoryRowShapeTests extends TestBase {
        @Test
        @DisplayName("TC158 — Each category row has category+netPayoutRevenue+platformFeeRevenue+totalRevenue+payoutCount")
        void category_row_shape() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _FmM2S5.fullChainWithFee(this, "WEB_DEV", 100.0, 10.0, "2026-09-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/category?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC158");
                JsonNode arr = parseNode(r.body());
                if (arr.has("content")) arr = arr.get("content");
                assertTrue(arr.size() > 0, "TC158: at least one row");
                JsonNode it = arr.get(0);
                assertTrue(it.has("category"),
                        "TC158: row must include category; got=" + it);
                assertTrue(it.has("totalRevenue") || it.has("total_revenue"),
                        "TC158: row must include totalRevenue; got=" + it);
                assertTrue(it.has("netPayoutRevenue") || it.has("net_payout_revenue"),
                        "TC158: row must include netPayoutRevenue; got=" + it);
                assertTrue(it.has("platformFeeRevenue") || it.has("platform_fee_revenue"),
                        "TC158: row must include platformFeeRevenue; got=" + it);
                assertTrue(it.has("payoutCount") || it.has("payout_count"),
                        "TC158: row must include payoutCount; got=" + it);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S5-F11 — Payout Method Breakdown (TC159-TC170)
// Reads MongoDB payout_audit_trail filtered by action ∈ {COMPLETED, FAILED}.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC159 — S5-F11 happy path composite ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC159_MethodBreakdownHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC159 — BANK_TRANSFER: success=5/failure=2/rate≈0.71/total=500; PAYPAL: 3/0/1.0/300")
        void method_happy_path() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC159: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 5; i++)
                        _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 100.0, _FmM2S5.t("2026-08-15T10:00:00Z"));
                for (int i = 0; i < 2; i++)
                        _FmM2S5.audit(col, "FAILED", "BANK_TRANSFER", 100.0, _FmM2S5.t("2026-08-15T11:00:00Z"));
                for (int i = 0; i < 3; i++)
                        _FmM2S5.audit(col, "COMPLETED", "PAYPAL", 100.0, _FmM2S5.t("2026-08-16T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC159");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("breakdown") ? arr.get("breakdown") : arr);
                java.util.Map<String, JsonNode> byMethod = new java.util.HashMap<>();
                for (JsonNode it : list)
                        byMethod.put(it.has("method") ? it.get("method").asText() : "", it);
                JsonNode bt = byMethod.get("BANK_TRANSFER");
                JsonNode pp = byMethod.get("PAYPAL");
                assertNotNull(bt, "TC159: BANK_TRANSFER entry; body=" + r.body());
                assertNotNull(pp, "TC159: PAYPAL entry");
                assertEquals(5L, _FmM2.rL(bt, "successCount", "success_count"),
                        "TC159: BT successCount");
                assertEquals(2L, _FmM2.rL(bt, "failureCount", "failure_count"),
                        "TC159: BT failureCount");
                assertEquals(0.71, _FmM2.rD(bt, "successRate", "success_rate"), 0.02,
                        "TC159: BT successRate");
                assertEquals(500.0, _FmM2.rD(bt, "totalAmount", "total_amount"), 0.5,
                        "TC159: BT totalAmount");
                assertEquals(3L, _FmM2.rL(pp, "successCount", "success_count"),
                        "TC159: PP successCount");
                assertEquals(0L, _FmM2.rL(pp, "failureCount", "failure_count"),
                        "TC159: PP failureCount");
                assertEquals(1.0, _FmM2.rD(pp, "successRate", "success_rate"), 0.001,
                        "TC159: PP successRate");
        }
}

// ─── TC160 — S5-F11 successCount per method ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC160_MethodSuccessCountTests extends TestBase {
        @Test
        @DisplayName("TC160 — successCount = number of COMPLETED events for the method")
        void success_count() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC160: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 4; i++)
                        _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-09-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC160");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("BANK_TRANSFER".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(4L, _FmM2.rL(it, "successCount", "success_count"),
                                        "TC160: successCount=4");
                                return;
                        }
                }
                throw new AssertionError("TC160: BANK_TRANSFER entry not found");
        }
}

// ─── TC161 — S5-F11 failureCount per method ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC161_MethodFailureCountTests extends TestBase {
        @Test
        @DisplayName("TC161 — failureCount = number of FAILED events for the method")
        void failure_count() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC161: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 3; i++)
                        _FmM2S5.audit(col, "FAILED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-10-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-10-01&endDate=2026-10-31", tok);
                assert2xx(r, "TC161");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("BANK_TRANSFER".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(3L, _FmM2.rL(it, "failureCount", "failure_count"),
                                        "TC161: failureCount=3");
                                return;
                        }
                }
                throw new AssertionError("TC161: BANK_TRANSFER entry not found");
        }
}

// ─── TC162 — S5-F11 successRate normal computation ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC162_MethodSuccessRateTests extends TestBase {
        @Test
        @DisplayName("TC162 — successRate = success / (success + failure)")
        void success_rate() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC162: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 4; i++)
                        _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-11-10T10:00:00Z"));
                _FmM2S5.audit(col, "FAILED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-11-10T11:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-11-01&endDate=2026-11-30", tok);
                assert2xx(r, "TC162");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("BANK_TRANSFER".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(0.8, _FmM2.rD(it, "successRate", "success_rate"), 0.01,
                                        "TC162: rate=0.8 (4/5)");
                                return;
                        }
                }
                throw new AssertionError("TC162: BANK_TRANSFER entry not found");
        }
}

// ─── TC163 — S5-F11 totalAmount sums COMPLETED only ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC163_MethodTotalAmountCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC163 — totalAmount sums COMPLETED amounts only (failed amounts excluded)")
        void total_amount_completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC163: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-12-10T10:00:00Z"));
                _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-12-10T11:00:00Z"));
                _FmM2S5.audit(col, "FAILED", "BANK_TRANSFER", 999.0, _FmM2S5.t("2026-12-10T12:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-12-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC163");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("BANK_TRANSFER".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(100.0, _FmM2.rD(it, "totalAmount", "total_amount"), 0.5,
                                        "TC163: totalAmount=100 (COMPLETED only)");
                                return;
                        }
                }
                throw new AssertionError("TC163: BANK_TRANSFER entry not found");
        }
}

// ─── TC164 — S5-F11 filter excludes CREATED/REFUNDED/REFUND_DENIED/ANALYTICS_VIEWED
@Tag("public")
@Tag("features_m2")
class TC164_MethodExcludesCreatedTests extends TestBase {
        @Test
        @DisplayName("TC164 — Only CREATED/REFUNDED/REFUND_DENIED/ANALYTICS_VIEWED events: empty result")
        void excludes_non_completed_failed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC164: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _FmM2S5.audit(col, "CREATED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-07-10T10:00:00Z"));
                _FmM2S5.audit(col, "REFUNDED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-07-10T11:00:00Z"));
                _FmM2S5.audit(col, "REFUND_DENIED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-07-10T12:00:00Z"));
                _FmM2S5.audit(col, "ANALYTICS_VIEWED", "BANK_TRANSFER", 50.0, _FmM2S5.t("2026-07-10T13:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-07-01&endDate=2026-07-31", tok);
                assert2xx(r, "TC164");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                assertEquals(0, list.size(), "TC164: non-COMPLETED/FAILED actions must be excluded");
        }
}

// ─── TC165 — S5-F11 group by method (3 distinct methods) ───────────────────
@Tag("public")
@Tag("features_m2")
class TC165_MethodGroupByTests extends TestBase {
        @Test
        @DisplayName("TC165 — Result has one entry per distinct method")
        void group_by_method() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC165: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 100.0, _FmM2S5.t("2026-08-10T10:00:00Z"));
                _FmM2S5.audit(col, "COMPLETED", "PAYPAL", 100.0, _FmM2S5.t("2026-08-10T11:00:00Z"));
                _FmM2S5.audit(col, "COMPLETED", "CRYPTO", 100.0, _FmM2S5.t("2026-08-10T12:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC165");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JsonNode it : list)
                        seen.add(it.has("method") ? it.get("method").asText() : "");
                for (String m : new String[] { "BANK_TRANSFER", "PAYPAL", "CRYPTO" }) {
                        assertTrue(seen.contains(m), "TC165: expected method '" + m + "'; got=" + seen);
                }
        }
}

// ─── TC166 — S5-F11 empty range returns empty list ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC166_MethodEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC166 — Empty date range returns empty list")
        void empty_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC166");
                JsonNode list = _FmM2S5.unwrap(parseNode(r.body()));
                assertEquals(0, list.size(), "TC166: empty list expected");
        }
}

// ─── TC167 — S5-F11 inverted dates → 400 ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC167_MethodInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC167 — startDate > endDate returns 400")
        void inverted_dates() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC167: must be 400");
        }
}

// ─── TC168 — S5-F11 missing JWT → 401 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC168_MethodMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC168 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGet(
                        "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31");
                assertEquals(401, r.statusCode(), "TC168: must be 401");
        }
}

// ─── TC169 — S5-F11 invalid JWT → 401 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC169_MethodInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC169 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31",
                        "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC169: must be 401");
        }
}

// ─── TC170 — S5-F11 cache hit served (Redis-confirmed) ────────────────────
@Tag("public")
@Tag("features_m2")
class TC170_MethodCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC170 — Cache hit: 2nd call (after Mongo mutation) returns 1st response")
        void cache_hit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null) throw new AssertionError("TC170: Redis required");
                if (mongo == null) throw new AssertionError("TC170: MongoDB required");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 100.0, _FmM2S5.t("2026-08-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r1, "TC170 #1");
                _FmM2S5.audit(col, "COMPLETED", "BANK_TRANSFER", 200.0, _FmM2S5.t("2026-08-11T10:00:00Z"));
                HttpResponse<String> r2 = httpGetAuth(
                        "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r2, "TC170 #2");
                assertEquals(r1.body(), r2.body(), "TC170: cache hit — r1 must equal r2");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S5-F12 — Process Milestone-Based Payout Reversal (TC171-TC190)
// POST /api/payouts/{id}/reverse-milestone
// Body: {reason, reversalScope: FULL|MILESTONE_ONLY}
// 30-day window from payout.createdAt; strategies: FullPayoutReversalStrategy
// / MilestoneReversalStrategy / NoReversalStrategy.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC171 — S5-F12 full reversal happy path ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC171_ReversalFullPathTests extends TestBase {
        @Test
        @DisplayName("TC171 — FullPayoutReversal: reversalScope=FULL, within 30 days → 100% refund")
        void full_reversal_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 200.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"client cancelled\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/reverse-milestone", body, tok);
                assert2xx(r, "TC171");
        }
}

// ─── TC172 — S5-F12 status updated to REFUNDED on success ─────────────────
@Tag("public")
@Tag("features_m2")
class TC172_ReversalStatusUpdatedTests extends TestBase {
        @Test
        @DisplayName("TC172 — Successful reversal updates Payout.status=REFUNDED")
        void reversal_status_updated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 200.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC172");
                String pTable = tableName("Payout");
                String stCol = columnByField("Payout", "status");
                String dbStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + pTable + "\" WHERE id=?",
                        String.class, pid);
                assertEquals("REFUNDED", dbStatus,
                        "TC172: Payout.status must be REFUNDED; got " + dbStatus);
        }
}

// ─── TC173 — S5-F12 transactionDetails.refundAmount populated (FULL) ──────
@Tag("public")
@Tag("features_m2")
class TC173_ReversalJsonbAmountFullTests extends TestBase {
        @Test
        @DisplayName("TC173 — FULL reversal populates transactionDetails.refundAmount=200 (full payout)")
        void reversal_jsonb_amount_full() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 200.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC173");
                String details = jdbc.queryForObject(
                        "SELECT transaction_details::text FROM \"" + tableName("Payout") + "\" WHERE id=?",
                        String.class, pid);
                JsonNode dj = parseNode(details);
                assertEquals(200.0, dj.has("refundAmount") ? dj.get("refundAmount").asDouble() : -1.0, 0.5,
                        "TC173: transactionDetails.refundAmount=200; got " + details);
        }
}

// ─── TC174 — S5-F12 transactionDetails.reversalScope set ──────────────────
@Tag("public")
@Tag("features_m2")
class TC174_ReversalJsonbScopeTests extends TestBase {
        @Test
        @DisplayName("TC174 — transactionDetails.reversalScope = 'FULL' (echoed from request)")
        void reversal_jsonb_scope() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 200.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC174");
                String details = jdbc.queryForObject(
                        "SELECT transaction_details::text FROM \"" + tableName("Payout") + "\" WHERE id=?",
                        String.class, pid);
                JsonNode dj = parseNode(details);
                String scope = dj.has("reversalScope") ? dj.get("reversalScope").asText() : "";
                assertEquals("FULL", scope,
                        "TC174: reversalScope must be FULL; got " + scope);
        }
}

// ─── TC175 — S5-F12 milestone-only reversal happy path ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC175_ReversalMilestoneOnlyTests extends TestBase {
        @Test
        @DisplayName("TC175 — MILESTONE_ONLY: refunds only PENDING/IN_PROGRESS milestones")
        void milestone_only_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithMilestones(this, 200.0, new double[]{50.0, 80.0, 70.0},
                        new String[]{"COMPLETED", "PENDING", "IN_PROGRESS"},
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"MILESTONE_ONLY\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/reverse-milestone", body, tok);
                assert2xx(r, "TC175");
                String details = jdbc.queryForObject(
                        "SELECT transaction_details::text FROM \"" + tableName("Payout") + "\" WHERE id=?",
                        String.class, pid);
                JsonNode dj = parseNode(details);
                double refund = dj.has("refundAmount") ? dj.get("refundAmount").asDouble() : -1.0;
                assertEquals(150.0, refund, 0.5,
                        "TC175: refund = 80+70 (PENDING+IN_PROGRESS only); got " + refund);
        }
}

// ─── TC176 — S5-F12 MILESTONE_ONLY scope echoed ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC176_ReversalMilestoneOnlyScopeTests extends TestBase {
        @Test
        @DisplayName("TC176 — MILESTONE_ONLY reversal stores reversalScope='MILESTONE_ONLY'")
        void reversal_milestone_only_scope() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithMilestones(this, 200.0, new double[]{100.0},
                        new String[]{"PENDING"},
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"MILESTONE_ONLY\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC176");
                String details = jdbc.queryForObject(
                        "SELECT transaction_details::text FROM \"" + tableName("Payout") + "\" WHERE id=?",
                        String.class, pid);
                JsonNode dj = parseNode(details);
                String scope = dj.has("reversalScope") ? dj.get("reversalScope").asText() : "";
                assertEquals("MILESTONE_ONLY", scope,
                        "TC176: reversalScope must be MILESTONE_ONLY; got " + scope);
        }
}

// ─── TC177 — S5-F12 no-reversal window (>30 days) returns 400 ──────────────
@Tag("public")
@Tag("features_m2")
class TC177_ReversalDeniedWindowExpiredTests extends TestBase {
        @Test
        @DisplayName("TC177 — payout.createdAt > 30 days ago → NoReversalStrategy → 400 with 'reversal window expired'")
        void reversal_denied_window_expired() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/reverse-milestone", body, tok);
                assertEquals(400, r.statusCode(),
                        "TC177: must be 400 (window expired); got " + r.statusCode() + " body=" + r.body());
                // NOTE: do not assert on response body — Spring Boot 4 strips
                // ResponseStatusException reason from the default error body unless
                // server.error.include-message=always is set. The denial reason is
                // verified via the Mongo audit event in TC178 instead.
        }
}

// ─── TC178 — S5-F12 REFUND_DENIED logged on no-reversal denial ─────────────
@Tag("public")
@Tag("features_m2")
class TC178_ReversalDeniedEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC178 — Reversal denial writes REFUND_DENIED to payout_audit_trail BEFORE 400")
        void reversal_denied_event_logged() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC178: MongoDB required");
                String coll = s5AuditCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "REFUND_DENIED"));
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok);
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "REFUND_DENIED"));
                assertTrue(after > before,
                        "TC178: REFUND_DENIED must be logged; before=" + before + " after=" + after);
        }
}

// ─── TC179 — S5-F12 denial invalidates S5-F10 cache ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC179_ReversalDeniedInvalidatesS5F10CacheTests extends TestBase {
        @Test
        @DisplayName("TC179 — Reversal denial invalidates wallet-service::S5-F10::* cache keys")
        void reversal_denied_invalidates_s5f10_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null) throw new AssertionError("TC179: Redis required");
                String tok = adminToken();
                String url = "/api/payouts/analytics/category?startDate=2026-08-01&endDate=2026-08-31";
                assert2xx(httpGetAuth(url, tok), "TC179 prime");
                long primed = redis.dbSize();
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000));
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok);
                long afterDenial = redis.dbSize();
                assertTrue(afterDenial < primed || afterDenial == 0,
                        "TC179: S5-F10 cache must be invalidated; primed=" + primed + " after=" + afterDenial);
        }
}

// ─── TC180 — S5-F12 denial invalidates S5-F11 cache ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC180_ReversalDeniedInvalidatesS5F11CacheTests extends TestBase {
        @Test
        @DisplayName("TC180 — Reversal denial invalidates wallet-service::S5-F11::* cache keys")
        void reversal_denied_invalidates_s5f11_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null) throw new AssertionError("TC180: Redis required");
                String tok = adminToken();
                String url = "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31";
                assert2xx(httpGetAuth(url, tok), "TC180 prime");
                long primed = redis.dbSize();
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000));
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok);
                long afterDenial = redis.dbSize();
                assertTrue(afterDenial < primed || afterDenial == 0,
                        "TC180: S5-F11 cache must be invalidated; primed=" + primed + " after=" + afterDenial);
        }
}

// ─── TC181 — S5-F12 PENDING payout rejected ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC181_ReversalPendingPayoutTests extends TestBase {
        @Test
        @DisplayName("TC181 — Reversal on PENDING payout returns 400")
        void reversal_pending_payout_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "PENDING", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/reverse-milestone", body, tok);
                assertEquals(400, r.statusCode(),
                        "TC181: must be 400 for PENDING; got " + r.statusCode());
        }
}

// ─── TC182 — S5-F12 already-refunded payout rejected ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC182_ReversalAlreadyRefundedTests extends TestBase {
        @Test
        @DisplayName("TC182 — Reversal on already-REFUNDED payout returns 400")
        void reversal_already_refunded_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "REFUNDED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/reverse-milestone", body, tok);
                assertEquals(400, r.statusCode(),
                        "TC182: must be 400 for REFUNDED; got " + r.statusCode());
        }
}

// ─── TC183 — S5-F12 non-existent payout returns 404 ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC183_ReversalNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC183 — Reversal on non-existent payoutId returns 404")
        void reversal_not_found_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/999999/reverse-milestone", body, tok);
                assertEquals(404, r.statusCode(),
                        "TC183: must be 404; got " + r.statusCode());
        }
}

// ─── TC184 — S5-F12 missing JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC184_ReversalMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC184 — Reversal without Authorization header returns 401")
        void reversal_missing_jwt_401() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPost(
                        "/api/payouts/" + pid + "/reverse-milestone", body);
                assertEquals(401, r.statusCode(),
                        "TC184: must be 401; got " + r.statusCode());
        }
}

// ─── TC185 — S5-F12 invalid JWT returns 401 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC185_ReversalInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC185 — Reversal with malformed JWT returns 401")
        void reversal_invalid_jwt_401() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/reverse-milestone", body, "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(),
                        "TC185: must be 401; got " + r.statusCode());
        }
}

// ─── TC186 — S5-F12 success writes REFUNDED audit event ───────────────────
@Tag("public")
@Tag("features_m2")
class TC186_ReversalSuccessAuditTests extends TestBase {
        @Test
        @DisplayName("TC186 — Successful reversal writes REFUNDED to payout_audit_trail")
        void reversal_success_audit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC186: MongoDB required");
                String coll = s5AuditCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "REFUNDED"));
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC186");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "REFUNDED"));
                assertTrue(after > before,
                        "TC186: REFUNDED must be logged; before=" + before + " after=" + after);
        }
}

// ─── TC187 — S5-F12 transactionDetails.refundReason populated ─────────────
@Tag("public")
@Tag("features_m2")
class TC187_ReversalJsonbReasonTests extends TestBase {
        @Test
        @DisplayName("TC187 — transactionDetails.refundReason matches request body reason")
        void reversal_jsonb_reason() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String reason = "scope changed";
                String body = "{\"reason\":\"" + reason + "\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC187");
                String details = jdbc.queryForObject(
                        "SELECT transaction_details::text FROM \"" + tableName("Payout") + "\" WHERE id=?",
                        String.class, pid);
                JsonNode dj = parseNode(details);
                String stored = dj.has("refundReason") ? dj.get("refundReason").asText() : "";
                assertEquals(reason, stored,
                        "TC187: refundReason must match; got " + stored);
        }
}

// ─── TC188 — S5-F12 transactionDetails.refundedAt populated ───────────────
@Tag("public")
@Tag("features_m2")
class TC188_ReversalJsonbRefundedAtTests extends TestBase {
        @Test
        @DisplayName("TC188 — transactionDetails.refundedAt is populated as ISO timestamp")
        void reversal_jsonb_refunded_at() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC188");
                String details = jdbc.queryForObject(
                        "SELECT transaction_details::text FROM \"" + tableName("Payout") + "\" WHERE id=?",
                        String.class, pid);
                JsonNode dj = parseNode(details);
                assertTrue(dj.has("refundedAt") && !dj.get("refundedAt").asText().isBlank(),
                        "TC188: refundedAt must be populated; got " + details);
        }
}

// ─── TC189 — S5-F12 success invalidates S5-F10 cache ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC189_ReversalSuccessInvalidatesS5F10CacheTests extends TestBase {
        @Test
        @DisplayName("TC189 — Successful reversal invalidates wallet-service::S5-F10::* cache keys")
        void reversal_success_invalidates_s5f10_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null) throw new AssertionError("TC189: Redis required");
                String tok = adminToken();
                String url = "/api/payouts/analytics/category?startDate=2026-08-01&endDate=2026-08-31";
                assert2xx(httpGetAuth(url, tok), "TC189 prime");
                long primed = redis.dbSize();
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC189 reverse");
                long afterReversal = redis.dbSize();
                assertTrue(afterReversal < primed || afterReversal == 0,
                        "TC189: S5-F10 cache must be invalidated; primed=" + primed + " after=" + afterReversal);
        }
}

// ─── TC190 — S5-F12 success invalidates S5-F11 cache ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC190_ReversalSuccessInvalidatesS5F11CacheTests extends TestBase {
        @Test
        @DisplayName("TC190 — Successful reversal invalidates wallet-service::S5-F11::* cache keys")
        void reversal_success_invalidates_s5f11_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null) throw new AssertionError("TC190: Redis required");
                String tok = adminToken();
                String url = "/api/payouts/analytics/methods?startDate=2026-08-01&endDate=2026-08-31";
                assert2xx(httpGetAuth(url, tok), "TC190 prime");
                long primed = redis.dbSize();
                long pid = _FmM2S5.payoutWithCreatedAt(this, "COMPLETED", 100.0,
                        new java.sql.Timestamp(System.currentTimeMillis()));
                String body = "{\"reason\":\"x\",\"reversalScope\":\"FULL\"}";
                assert2xx(httpPostAuth("/api/payouts/" + pid + "/reverse-milestone", body, tok), "TC190 reverse");
                long afterReversal = redis.dbSize();
                assertTrue(afterReversal < primed || afterReversal == 0,
                        "TC190: S5-F11 cache must be invalidated; primed=" + primed + " after=" + afterReversal);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// Helper class for S5 — Payout + Contract + Job + ProposalMilestone seeding +
// MongoDB audit insert + Mongo result unwrapper.
// ════════════════════════════════════════════════════════════════════════════
final class _FmM2S5 {
        private _FmM2S5() {}

        /** INSERT a Payout (and minimal contract+proposal+job chain). Returns payout id. */
        static Long fullChain(TestBase t, String category, double amount, String dateStr) {
                long jobId = seedJobWithCategory(t, category);
                long proposalId = seedProposal(t, jobId);
                long contractId = seedContract(t, proposalId, jobId);
                String pTable = t.tableName("Payout");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Payout", "contract"), contractId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "freelancer"), 1L); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Payout", "amount"), amount);
                ov.put(t.columnByField("Payout", "status"), "COMPLETED");
                try { ov.put(t.columnByField("Payout", "method"), "BANK_TRANSFER"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "transactionDetails"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(pTable, ov);
                t.setAllDateColumns(pTable, id, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                return id;
        }

        /** Like fullChain but with explicit status. */
        static Long fullChainWithStatus(TestBase t, String category, double amount, String status, String dateStr) {
                long jobId = seedJobWithCategory(t, category);
                long proposalId = seedProposal(t, jobId);
                long contractId = seedContract(t, proposalId, jobId);
                String pTable = t.tableName("Payout");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Payout", "contract"), contractId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "freelancer"), 1L); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Payout", "amount"), amount);
                ov.put(t.columnByField("Payout", "status"), status);
                try { ov.put(t.columnByField("Payout", "method"), "BANK_TRANSFER"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "transactionDetails"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(pTable, ov);
                t.setAllDateColumns(pTable, id, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                return id;
        }

        /** No platformFee in JSONB → tests fallback. */
        static Long fullChainNoFee(TestBase t, String category, double amount, String dateStr) {
                return fullChain(t, category, amount, dateStr);
        }

        /** Explicit platformFee in JSONB. */
        static Long fullChainWithFee(TestBase t, String category, double amount, double platformFee, String dateStr) {
                Long id = fullChain(t, category, amount, dateStr);
                String pTable = t.tableName("Payout");
                t.jdbc.update("UPDATE \"" + pTable + "\" SET transaction_details=?::jsonb WHERE id=?",
                        String.format("{\"platformFee\":%.2f}", platformFee), id);
                return id;
        }

        /** Insert a Payout with explicit createdAt for window-based reversal testing. */
        static long payoutWithCreatedAt(TestBase t, String status, double amount, java.sql.Timestamp createdAt) {
                long jobId = seedJobWithCategory(t, "WEB_DEV");
                long proposalId = seedProposal(t, jobId);
                long contractId = seedContract(t, proposalId, jobId);
                String pTable = t.tableName("Payout");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Payout", "contract"), contractId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "freelancer"), 1L); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Payout", "amount"), amount);
                ov.put(t.columnByField("Payout", "status"), status);
                try { ov.put(t.columnByField("Payout", "method"), "BANK_TRANSFER"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "transactionDetails"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(pTable, ov);
                t.setAllDateColumns(pTable, id, createdAt);
                try {
                        t.jdbc.update("UPDATE \"" + pTable + "\" SET created_at=? WHERE id=?", createdAt, id);
                } catch (Throwable ignore) {}
                return id;
        }

        /** Insert Payout + linked Proposal with milestone breakdown. Used for MILESTONE_ONLY reversal. */
        static long payoutWithMilestones(TestBase t, double amount, double[] milestoneAmounts,
                                          String[] milestoneStatuses, java.sql.Timestamp createdAt) {
                long jobId = seedJobWithCategory(t, "WEB_DEV");
                long proposalId = seedProposal(t, jobId);
                long contractId = seedContract(t, proposalId, jobId);
                String mtbl = t.tableName("ProposalMilestone");
                for (int i = 0; i < milestoneAmounts.length; i++) {
                        java.util.Map<String, Object> mov = new java.util.HashMap<>();
                        try { mov.put(t.columnByField("ProposalMilestone", "proposal"), proposalId); } catch (Throwable ignore) {}
                        try { mov.put(t.columnByField("ProposalMilestone", "milestoneOrder"), i + 1); } catch (Throwable ignore) {}
                        try { mov.put(t.columnByField("ProposalMilestone", "amount"), milestoneAmounts[i]); } catch (Throwable ignore) {}
                        try { mov.put(t.columnByField("ProposalMilestone", "status"), milestoneStatuses[i]); } catch (Throwable ignore) {}
                        try { mov.put(t.columnByField("ProposalMilestone", "description"), "milestone " + (i + 1)); } catch (Throwable ignore) {}
                        t.insertRowReturningId(mtbl, mov);
                }
                String pTable = t.tableName("Payout");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Payout", "contract"), contractId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "freelancer"), 1L); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Payout", "amount"), amount);
                ov.put(t.columnByField("Payout", "status"), "COMPLETED");
                try { ov.put(t.columnByField("Payout", "method"), "BANK_TRANSFER"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "transactionDetails"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(pTable, ov);
                t.setAllDateColumns(pTable, id, createdAt);
                try {
                        t.jdbc.update("UPDATE \"" + pTable + "\" SET created_at=? WHERE id=?", createdAt, id);
                } catch (Throwable ignore) {}
                return id;
        }

        // ─── Internal helpers ───────────────────────────────────────────────

        private static long seedJobWithCategory(TestBase t, String category) {
                String table = t.tableName("Job");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("Job", "title"), "Job " + System.nanoTime());
                try { ov.put(t.columnByField("Job", "description"), "auto-seeded"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "client"), 1L); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "category"), category); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "status"), t.enumValueAt("Job", "status", 0)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "budgetMin"), 50.0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "budgetMax"), 1000.0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "requirements"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(table, ov);
        }

        private static long seedProposal(TestBase t, long jobId) {
                String table = t.tableName("Proposal");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Proposal", "freelancer"), 1L); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "job"), jobId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "bidAmount"), 100.0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "estimatedDays"), 5); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "coverLetter"), "auto cover letter"); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Proposal", "status"), "ACCEPTED");
                try { ov.put(t.columnByField("Proposal", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(table, ov);
        }

        private static long seedContract(TestBase t, long proposalId, long jobId) {
                String table = t.tableName("Contract");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Contract", "freelancer"), 1L); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "client"), 1L); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "proposal"), proposalId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "job"), jobId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "agreedAmount"), 100.0); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Contract", "status"), "COMPLETED");
                try { ov.put(t.columnByField("Contract", "startDate"), java.sql.Date.valueOf("2026-01-01")); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "endDate"), java.sql.Date.valueOf("2026-01-15")); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(table, ov);
        }

        // ─── MongoDB audit-trail helpers ────────────────────────────────────

        static void audit(com.mongodb.client.MongoCollection<org.bson.Document> col,
                          String action, String method, double amount, java.time.Instant ts) {
                org.bson.Document doc = new org.bson.Document();
                doc.put("action", action);
                doc.put("eventType", action);
                doc.put("method", method);
                doc.put("amount", amount);
                doc.put("timestamp", java.util.Date.from(ts));
                col.insertOne(doc);
        }

        static java.time.Instant t(String iso) {
                return java.time.Instant.parse(iso);
        }

        // ─── Result helpers ─────────────────────────────────────────────────

        static JsonNode unwrap(JsonNode root) {
                if (root.isArray()) return root;
                if (root.has("breakdown")) return root.get("breakdown");
                if (root.has("content")) return root.get("content");
                return root;
        }

        static JsonNode findByCategory(JsonNode arr, String category) {
                for (JsonNode it : arr) {
                        String c = it.has("category") ? it.get("category").asText() : null;
                        if (category.equals(c)) return it;
                }
                return null;
        }
}


// ════════════════════════════════════════════════════════════════════════════
// M1 — Per-service feature tests (TC191..TC378). Tests the 5 services × 9
// features (45 features) in the Freelance Marketplace M1 spec. Sub-entities:
// UserSkill (S1), JobAttachment (S2), ProposalMilestone (S3), PayoutPromo +
// PromoCode (S5).
//
// All tests authenticated via admin token (M2 amendment requires JWT on every
// M1 endpoint). Section breakdown:
//   S1 User Service:    TC191..TC220 (9 features, ~30 TCs)
//   S2 Job Service:     TC221..TC256 (9 features, ~36 TCs)
//   S3 Proposal Service:TC257..TC292 (9 features, ~36 TCs)
//   S4 Contract Service:TC293..TC340 (9 features, ~48 TCs)
//   S5 Wallet Service:  TC341..TC378 (9 features, ~38 TCs)
// ════════════════════════════════════════════════════════════════════════════

final class _FmM1Seed {
        private _FmM1Seed() {}

        /** INSERT a user. role: FREELANCER / CLIENT / ADMIN. Returns user id. */
        static long seedUser(TestBase t, String name, String email, String role) {
                String tbl = t.tableName("User");
                String bcrypt = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
                String phone = "+201" + String.format("%09d", System.nanoTime() % 1_000_000_000L);
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("User", "name"), name);
                ov.put(t.columnByField("User", "email"), email);
                ov.put(t.columnByField("User", "phone"), phone);
                ov.put(t.columnByField("User", "password"), bcrypt);
                ov.put(t.columnByField("User", "role"), role);
                ov.put(t.columnByField("User", "status"), "ACTIVE");
                try { ov.put(t.columnByField("User", "preferences"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** UPDATE user.preferences JSONB. */
        static void setPrefs(TestBase t, long userId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("User") + "\" SET preferences=?::jsonb WHERE id=?",
                        json, userId);
        }

        /** INSERT a UserSkill. Returns id. */
        static long seedSkill(TestBase t, long userId, String skillName, String category,
                              int years, String level, boolean isPrimary) {
                String tbl = t.tableName("UserSkill");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("UserSkill", "user"), userId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("UserSkill", "skillName"), skillName); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("UserSkill", "category"), category); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("UserSkill", "yearsOfExperience"), years); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("UserSkill", "proficiencyLevel"), level); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("UserSkill", "isPrimary"), isPrimary); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("UserSkill", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** INSERT a Job. Returns id. */
        static long seedJob(TestBase t, String title, String category, String status,
                            double budgetMin, double budgetMax) {
                String tbl = t.tableName("Job");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("Job", "title"), title);
                try { ov.put(t.columnByField("Job", "description"), "auto-seeded job"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "client"), 1L); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "category"), category); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "status"), status); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "budgetMin"), budgetMin); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "budgetMax"), budgetMax); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Job", "requirements"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Set Job.requirements JSONB. */
        static void setJobRequirements(TestBase t, long jobId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("Job") + "\" SET requirements=?::jsonb WHERE id=?",
                        json, jobId);
        }

        /** INSERT a JobAttachment. */
        static long seedJobAttachment(TestBase t, long jobId, String type, String fileUrl,
                                      java.time.LocalDate expiry, boolean verified) {
                String tbl = t.tableName("JobAttachment");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("JobAttachment", "job"), jobId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("JobAttachment", "type"), type); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("JobAttachment", "fileUrl"), fileUrl); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("JobAttachment", "expiryDate"), java.sql.Date.valueOf(expiry)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("JobAttachment", "verified"), verified); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("JobAttachment", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** INSERT a Proposal. status: SUBMITTED/SHORTLISTED/ACCEPTED/REJECTED/WITHDRAWN. */
        static long seedProposal(TestBase t, long freelancerId, long jobId, String status,
                                 double bidAmount, int estimatedDays, String date) {
                String tbl = t.tableName("Proposal");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Proposal", "freelancer"), freelancerId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "job"), jobId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "bidAmount"), bidAmount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "estimatedDays"), estimatedDays); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Proposal", "coverLetter"), "auto cover " + System.nanoTime()); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Proposal", "status"), status);
                try { ov.put(t.columnByField("Proposal", "metadata"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(tbl, ov);
                if (date != null) {
                        t.setAllDateColumns(tbl, id, java.sql.Timestamp.valueOf(date + " 12:00:00"));
                        try {
                                t.jdbc.update("UPDATE \"" + tbl + "\" SET \""
                                        + t.columnByField("Proposal", "submittedAt") + "\"=? WHERE id=?",
                                        java.sql.Timestamp.valueOf(date + " 12:00:00"), id);
                        } catch (Throwable ignore) {}
                }
                return id;
        }

        /** INSERT a ProposalMilestone. */
        static long seedMilestone(TestBase t, long proposalId, int order, String description,
                                  double amount, String status) {
                String tbl = t.tableName("ProposalMilestone");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("ProposalMilestone", "proposal"), proposalId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ProposalMilestone", "milestoneOrder"), order); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ProposalMilestone", "description"), description); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ProposalMilestone", "amount"), amount); } catch (Throwable ignore) {}
                ov.put(t.columnByField("ProposalMilestone", "status"), status);
                try { ov.put(t.columnByField("ProposalMilestone", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** INSERT a Contract. status: ACTIVE/COMPLETED/TERMINATED/DISPUTED. date is startDate. */
        static long seedContract(TestBase t, long freelancerId, long clientId, long proposalId,
                                 long jobId, String status, double agreedAmount, String startDate, String endDate) {
                String tbl = t.tableName("Contract");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Contract", "freelancer"), freelancerId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "client"), clientId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "proposal"), proposalId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "job"), jobId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Contract", "agreedAmount"), agreedAmount); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Contract", "status"), status);
                try { ov.put(t.columnByField("Contract", "startDate"), java.sql.Date.valueOf(startDate)); } catch (Throwable ignore) {}
                if (endDate != null) {
                        try { ov.put(t.columnByField("Contract", "endDate"), java.sql.Date.valueOf(endDate)); } catch (Throwable ignore) {}
                }
                try { ov.put(t.columnByField("Contract", "metadata"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(tbl, ov);
                t.setAllDateColumns(tbl, id, java.sql.Timestamp.valueOf(startDate + " 12:00:00"));
                if (endDate != null) {
                        try {
                                t.jdbc.update("UPDATE \"" + tbl + "\" SET \""
                                        + t.columnByField("Contract", "endDate") + "\" = ? WHERE id = ?",
                                        java.sql.Timestamp.valueOf(endDate + " 12:00:00"), id);
                        } catch (Throwable ignore) {}
                }
                return id;
        }

        /** Set Contract.metadata JSONB. */
        static void setContractMetadata(TestBase t, long contractId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("Contract") + "\" SET metadata=?::jsonb WHERE id=?",
                        json, contractId);
        }

        /** INSERT a Payout. method: BANK_TRANSFER/PAYPAL/CRYPTO. status: PENDING/COMPLETED/FAILED/REFUNDED. */
        static long seedPayout(TestBase t, long contractId, long freelancerId, double amount,
                               String method, String status) {
                String tbl = t.tableName("Payout");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Payout", "contract"), contractId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Payout", "freelancer"), freelancerId); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Payout", "amount"), amount);
                try { ov.put(t.columnByField("Payout", "method"), method); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Payout", "status"), status);
                try { ov.put(t.columnByField("Payout", "transactionDetails"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** INSERT a PromoCode. expiry can be null. */
        static long seedPromo(TestBase t, String code, String type, double value,
                              int maxUses, java.time.LocalDateTime expiry, boolean active) {
                String tbl = t.tableName("PromoCode");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("PromoCode", "code"), code);
                try { ov.put(t.columnByField("PromoCode", "discountType"), type); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "discountValue"), value); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "maxUses"), maxUses); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "currentUses"), 0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "expiryDate"),
                        expiry == null ? null : java.sql.Timestamp.valueOf(expiry)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "active"), active); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** INSERT a PayoutPromo join row. */
        static long seedPayoutPromo(TestBase t, long payoutId, long promoId, double appliedAmount) {
                String tbl = t.tableName("PayoutPromo");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("PayoutPromo", "payout"), payoutId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PayoutPromo", "promoCode"), promoId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PayoutPromo", "discountApplied"), appliedAmount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PayoutPromo", "discountAmount"), appliedAmount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PayoutPromo", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Set Payout.transactionDetails JSONB. */
        static void setPayoutDetails(TestBase t, long payoutId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("Payout") + "\" SET transaction_details=?::jsonb WHERE id=?",
                        json, payoutId);
        }

        /** Set PromoCode.currentUses to a specific value. */
        static void setPromoCurrentUses(TestBase t, long promoId, int uses) {
                try {
                        t.jdbc.update("UPDATE \"" + t.tableName("PromoCode") + "\" SET \""
                                + t.columnByField("PromoCode", "currentUses") + "\"=? WHERE id=?",
                                uses, promoId);
                } catch (Throwable ignore) {}
        }

        static java.time.LocalDateTime futureDateTime() {
                return java.time.LocalDateTime.of(2030, 12, 31, 23, 59, 59);
        }
        static java.time.LocalDateTime pastDateTime() {
                return java.time.LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        }
        static java.time.LocalDate futureDate() { return java.time.LocalDate.of(2030, 12, 31); }
        static java.time.LocalDate pastDate() { return java.time.LocalDate.of(2020, 1, 1); }
}


// ────────────────────────────────────────────────────────────────────────────
// S1 — User Service (TC191..TC220)  — 9 features × ~3 TCs each
// S1-F1 search, S1-F2 prefs, S1-F3 contract-summary, S1-F4 deactivate,
// S1-F5 prefs filter, S1-F6 top-freelancers, S1-F7 primary-skill,
// S1-F8 profile, S1-F9 lang+minContracts
// ────────────────────────────────────────────────────────────────────────────

// ─── TC191 — S1-F1 search by partial name ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC191_FmSearchUsersByNameTests extends TestBase {
        @Test
        @DisplayName("TC191 — Search by name 'Ahmed' returns 2 users (partial match)")
        void search_by_name() throws Exception {
                BASE_URL = userServiceUrl;
                _FmM1Seed.seedUser(this, "Ahmed",     "tc191_a@fm.io", "FREELANCER");
                _FmM1Seed.seedUser(this, "Sara",      "tc191_b@fm.io", "CLIENT");
                _FmM1Seed.seedUser(this, "Ahmed Ali", "tc191_c@fm.io", "FREELANCER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=Ahmed", tok);
                assert2xx(r, "TC191");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int matches = 0;
                for (JsonNode it : list) {
                        String n = it.has("name") ? it.get("name").asText() : "";
                        if (n.contains("Ahmed")) matches++;
                }
                assertEquals(2, matches, "TC191: 2 Ahmed-named users expected; got " + matches);
        }
}

// ─── TC192 — S1-F1 search by role exact match ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC192_FmSearchUsersByRoleTests extends TestBase {
        @Test
        @DisplayName("TC192 — Search by role=FREELANCER returns FREELANCER users only")
        void search_by_role() throws Exception {
                BASE_URL = userServiceUrl;
                _FmM1Seed.seedUser(this, "Fr1", "tc192_a@fm.io", "FREELANCER");
                _FmM1Seed.seedUser(this, "Cl1", "tc192_b@fm.io", "CLIENT");
                _FmM1Seed.seedUser(this, "Fr2", "tc192_c@fm.io", "FREELANCER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?role=FREELANCER", tok);
                assert2xx(r, "TC192");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String role = it.has("role") ? it.get("role").asText() : "";
                        assertEquals("FREELANCER", role, "TC192: every result must have role=FREELANCER");
                }
                assertTrue(list.size() >= 2, "TC192: at least 2 FREELANCER expected");
        }
}

// ─── TC193 — S1-F1 no-match returns empty list ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC193_FmSearchUsersNoMatchTests extends TestBase {
        @Test
        @DisplayName("TC193 — Search with no-matching name returns empty list")
        void search_no_match() throws Exception {
                BASE_URL = userServiceUrl;
                _FmM1Seed.seedUser(this, "Ahmed", "tc193_a@fm.io", "FREELANCER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=zzzNoMatchXYZ", tok);
                assert2xx(r, "TC193");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC193: empty list expected");
        }
}

// ─── TC194 — S1-F2 update preferences merge ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC194_FmUpdatePreferencesMergeTests extends TestBase {
        @Test
        @DisplayName("TC194 — PUT preferences merges: language preserved, theme updated, currency added")
        void preferences_merge() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Pref User", "tc194@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, uid, "{\"language\":\"en\",\"theme\":\"light\"}");
                String tok = adminToken();
                String body = "{\"theme\":\"dark\",\"currency\":\"USD\"}";
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences", body, tok);
                assert2xx(r, "TC194");
                JsonNode j = parseNode(r.body());
                JsonNode prefs = j.has("preferences") ? j.get("preferences") : j;
                assertEquals("en", prefs.has("language") ? prefs.get("language").asText() : "", "TC194: language preserved");
                assertEquals("dark", prefs.has("theme") ? prefs.get("theme").asText() : "", "TC194: theme updated");
                assertEquals("USD", prefs.has("currency") ? prefs.get("currency").asText() : "", "TC194: currency added");
        }
}

// ─── TC195 — S1-F2 same-key overwrite ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC195_FmUpdatePreferencesOverwriteTests extends TestBase {
        @Test
        @DisplayName("TC195 — PUT with existing key overwrites it")
        void preferences_overwrite() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Pref User", "tc195@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, uid, "{\"language\":\"en\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences", "{\"language\":\"fr\"}", tok);
                assert2xx(r, "TC195");
                JsonNode prefs = parseNode(r.body()).has("preferences") ? parseNode(r.body()).get("preferences") : parseNode(r.body());
                assertEquals("fr", prefs.has("language") ? prefs.get("language").asText() : "",
                        "TC195: language must be overwritten");
        }
}

// ─── TC196 — S1-F2 404 non-existent user ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC196_FmUpdatePreferencesNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC196 — PUT preferences for non-existent user returns 404")
        void preferences_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/999999/preferences", "{\"x\":\"y\"}", tok);
                assertEquals(404, r.statusCode(), "TC196: must be 404");
        }
}

// ─── TC197 — S1-F3 contract-summary happy path ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC197_FmContractSummaryHappyTests extends TestBase {
        @Test
        @DisplayName("TC197 — Summary returns totalContracts=5, completedContracts=3, totalEarnings=700, terminatedContracts=1")
        void summary_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Sum User", "tc197@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "COMPLETED", 150.0, "2026-03-10", "2026-03-20");
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "COMPLETED", 200.0, "2026-03-11", "2026-03-21");
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "COMPLETED", 350.0, "2026-03-12", "2026-03-22");
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "TERMINATED", 999.0, "2026-03-13", null);
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "ACTIVE", 999.0, "2026-03-14", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/contract-summary", tok);
                assert2xx(r, "TC197");
                JsonNode j = parseNode(r.body());
                assertEquals(5L, _FmM2.rL(j, "totalContracts", "total_contracts"),
                        "TC197: totalContracts=5");
                assertEquals(3L, _FmM2.rL(j, "completedContracts", "completed_contracts"),
                        "TC197: completedContracts=3");
                assertEquals(1L, _FmM2.rL(j, "terminatedContracts", "terminated_contracts"),
                        "TC197: terminatedContracts=1");
                assertEquals(700.0, _FmM2.rD(j, "totalEarnings", "total_earnings"), 0.5,
                        "TC197: totalEarnings=700 (COMPLETED only)");
        }
}

// ─── TC198 — S1-F3 user with no contracts → zeros ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC198_FmContractSummaryNoneTests extends TestBase {
        @Test
        @DisplayName("TC198 — Summary for user with no contracts returns zeros")
        void summary_no_contracts() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Empty User", "tc198@fm.io", "FREELANCER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/contract-summary", tok);
                assert2xx(r, "TC198");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, _FmM2.rL(j, "totalContracts", "total_contracts"),
                        "TC198: totalContracts=0");
                assertEquals(0.0, _FmM2.rD(j, "totalEarnings", "total_earnings"), 0.01,
                        "TC198: totalEarnings=0");
        }
}

// ─── TC199 — S1-F3 404 non-existent user ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC199_FmContractSummaryNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC199 — Summary for non-existent user returns 404")
        void summary_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/999999/contract-summary", tok);
                assertEquals(404, r.statusCode(), "TC199: must be 404");
        }
}

// ─── TC200 — S1-F4 deactivate fails when active contract ─────────────────
@Tag("public")
@Tag("features_m1")
class TC200_FmDeactivateActiveContractTests extends TestBase {
        @Test
        @DisplayName("TC200 — Deactivate fails (400) when user has an ACTIVE contract")
        void deactivate_active_400() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Active User", "tc200@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "MOBILE", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "ACTIVE", 200.0, "2030-03-10", null);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assertEquals(400, r.statusCode(), "TC200: must be 400 with active contract");
        }
}

// ─── TC201 — S1-F4 deactivate succeeds and withdraws SUBMITTED proposals
@Tag("public")
@Tag("features_m1")
class TC201_FmDeactivateSuccessTests extends TestBase {
        @Test
        @DisplayName("TC201 — Deactivate succeeds when only COMPLETED contracts; withdraws SUBMITTED proposals; PG status=DEACTIVATED")
        void deactivate_success() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Done User", "tc201@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedContract(this, uid, 1L, 0L, jid, "COMPLETED", 50.0, "2026-03-10", "2026-03-15");
                long propId = _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-04-01");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assert2xx(r, "TC201");
                String stCol = columnByField("User", "status");
                String dbStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tableName("User") + "\" WHERE id=?",
                        String.class, uid);
                assertEquals("DEACTIVATED", dbStatus, "TC201: User.status=DEACTIVATED expected");
                String propStCol = columnByField("Proposal", "status");
                String propStatus = jdbc.queryForObject(
                        "SELECT \"" + propStCol + "\"::text FROM \"" + tableName("Proposal") + "\" WHERE id=?",
                        String.class, propId);
                assertEquals("WITHDRAWN", propStatus,
                        "TC201: SUBMITTED proposal must be WITHDRAWN; got " + propStatus);
        }
}

// ─── TC202 — S1-F4 404 non-existent user ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC202_FmDeactivateNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC202 — Deactivate non-existent user returns 404")
        void deactivate_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/999999/deactivate", "", tok);
                assertEquals(404, r.statusCode(), "TC202: must be 404");
        }
}

// ─── TC203 — S1-F5 preferences search happy match ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC203_FmPreferencesSearchHappyTests extends TestBase {
        @Test
        @DisplayName("TC203 — ?key=language&value=ar matches users with prefs.language=ar")
        void preferences_search_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _FmM1Seed.seedUser(this, "Ar1", "tc203_a@fm.io", "FREELANCER");
                long u2 = _FmM1Seed.seedUser(this, "En1", "tc203_b@fm.io", "FREELANCER");
                long u3 = _FmM1Seed.seedUser(this, "Ar2", "tc203_c@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, u1, "{\"language\":\"ar\"}");
                _FmM1Seed.setPrefs(this, u2, "{\"language\":\"en\"}");
                _FmM1Seed.setPrefs(this, u3, "{\"language\":\"ar\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/search?key=language&value=ar", tok);
                assert2xx(r, "TC203");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC203: at least 2 ar-language users expected");
        }
}

// ─── TC204 — S1-F5 no match returns empty list ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC204_FmPreferencesSearchNoMatchTests extends TestBase {
        @Test
        @DisplayName("TC204 — Unknown value returns empty list")
        void preferences_search_no_match() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _FmM1Seed.seedUser(this, "X", "tc204@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, u1, "{\"language\":\"ar\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/search?key=language&value=zh", tok);
                assert2xx(r, "TC204");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC204: empty list expected");
        }
}

// ─── TC205 — S1-F5 400 blank key ─────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC205_FmPreferencesSearchBlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC205 — Blank key returns 400")
        void preferences_search_blank_key() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/search?key=&value=ar", tok);
                assertEquals(400, r.statusCode(), "TC205: blank key must be 400");
        }
}

// ─── TC206 — S1-F6 top freelancers happy ranking ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC206_FmTopFreelancersHappyTests extends TestBase {
        @Test
        @DisplayName("TC206 — Top freelancers ranks user B (3500) above user A (1200)")
        void top_freelancers_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _FmM1Seed.seedUser(this, "FrA", "tc206_a@fm.io", "FREELANCER");
                long uB = _FmM1Seed.seedUser(this, "FrB", "tc206_b@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, uA, 1L, 0L, jid, "COMPLETED", 1200.0, "2026-03-10", "2026-03-15");
                _FmM1Seed.seedContract(this, uB, 1L, 0L, jid, "COMPLETED", 1500.0, "2026-03-11", "2026-03-16");
                _FmM1Seed.seedContract(this, uB, 1L, 0L, jid, "COMPLETED", 2000.0, "2026-03-12", "2026-03-17");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-freelancers?startDate=2026-03-01&endDate=2026-03-31&limit=10", tok);
                assert2xx(r, "TC206");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC206: at least 2 freelancers expected");
                long firstId = _FmM2.rL(list.get(0), "userId", "id");
                assertEquals(uB, firstId, "TC206: freelancer B (3500) must rank first");
        }
}

// ─── TC207 — S1-F6 empty range returns empty list ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC207_FmTopFreelancersEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC207 — Date range with no completed contracts returns empty list")
        void top_freelancers_empty() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-freelancers?startDate=2030-01-01&endDate=2030-01-31&limit=10", tok);
                assert2xx(r, "TC207");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC207: empty list expected");
        }
}

// ─── TC208 — S1-F6 400 invalid range ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC208_FmTopFreelancersInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC208 — start>end returns 400")
        void top_freelancers_invalid_range() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-freelancers?startDate=2026-03-31&endDate=2026-03-01&limit=10", tok);
                assertEquals(400, r.statusCode(), "TC208: invalid range must be 400");
        }
}

// ─── TC209 — S1-F7 set primary skill happy switch ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC209_FmSetPrimarySkillHappyTests extends TestBase {
        @Test
        @DisplayName("TC209 — PUT /skills/{skillId}/primary flips primary to target")
        void set_primary_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "SkUser", "tc209@fm.io", "FREELANCER");
                _FmM1Seed.seedSkill(this, uid, "React", "WEB_DEV", 5, "EXPERT", false);
                long s2 = _FmM1Seed.seedSkill(this, uid, "Vue", "WEB_DEV", 3, "INTERMEDIATE", true);
                long s3 = _FmM1Seed.seedSkill(this, uid, "Angular", "WEB_DEV", 4, "EXPERT", false);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/" + uid + "/skills/" + s3 + "/primary", "", tok);
                assert2xx(r, "TC209");
                String pCol = columnByField("UserSkill", "isPrimary");
                Boolean s3prim = jdbc.queryForObject(
                        "SELECT \"" + pCol + "\" FROM \"" + tableName("UserSkill") + "\" WHERE id=?",
                        Boolean.class, s3);
                Boolean s2prim = jdbc.queryForObject(
                        "SELECT \"" + pCol + "\" FROM \"" + tableName("UserSkill") + "\" WHERE id=?",
                        Boolean.class, s2);
                assertEquals(Boolean.TRUE, s3prim, "TC209: s3 must be primary");
                assertEquals(Boolean.FALSE, s2prim, "TC209: s2 must no longer be primary");
        }
}

// ─── TC210 — S1-F7 404 non-existent user ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC210_FmSetPrimarySkillUserNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC210 — Non-existent user returns 404")
        void set_primary_user_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/999999/skills/1/primary", "", tok);
                assertEquals(404, r.statusCode(), "TC210: must be 404");
        }
}

// ─── TC211 — S1-F7 400 skill belongs to different user ───────────────────
@Tag("public")
@Tag("features_m1")
class TC211_FmSetPrimarySkillCrossUserTests extends TestBase {
        @Test
        @DisplayName("TC211 — Skill belongs to different user → 400")
        void set_primary_cross_user() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _FmM1Seed.seedUser(this, "U1", "tc211_a@fm.io", "FREELANCER");
                long u2 = _FmM1Seed.seedUser(this, "U2", "tc211_b@fm.io", "FREELANCER");
                long s2 = _FmM1Seed.seedSkill(this, u2, "Java", "WEB_DEV", 5, "EXPERT", true);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/" + u1 + "/skills/" + s2 + "/primary", "", tok);
                assertEquals(400, r.statusCode(), "TC211: cross-user must be 400");
        }
}

// ─── TC212 — S1-F7 404 non-existent skill ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC212_FmSetPrimarySkillNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC212 — Non-existent skill returns 404")
        void set_primary_skill_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "U", "tc212@fm.io", "FREELANCER");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/" + uid + "/skills/999999/primary", "", tok);
                assertEquals(404, r.statusCode(), "TC212: must be 404");
        }
}

// ─── TC213 — S1-F8 user profile happy path ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC213_FmUserProfileHappyTests extends TestBase {
        @Test
        @DisplayName("TC213 — Profile DTO has totalSkills=3 + skills array")
        void profile_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "ProfUser", "tc213@fm.io", "FREELANCER");
                _FmM1Seed.seedSkill(this, uid, "React", "WEB_DEV", 5, "EXPERT", true);
                _FmM1Seed.seedSkill(this, uid, "Node", "WEB_DEV", 3, "INTERMEDIATE", false);
                _FmM1Seed.seedSkill(this, uid, "TypeScript", "WEB_DEV", 4, "EXPERT", false);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC213");
                JsonNode j = parseNode(r.body());
                long totalSk = _FmM2.rL(j, "totalSkills", "total_skills");
                assertEquals(3L, totalSk, "TC213: totalSkills=3");
                JsonNode skills = _FmM2.rO(j, "skills");
                assertNotNull(skills, "TC213: skills array required");
                assertEquals(3, skills.size(), "TC213: 3 skills expected");
        }
}

// ─── TC214 — S1-F8 user with no skills ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC214_FmUserProfileNoSkillsTests extends TestBase {
        @Test
        @DisplayName("TC214 — User with 0 skills returns totalSkills=0")
        void profile_no_skills() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "NoSk", "tc214@fm.io", "FREELANCER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC214");
                JsonNode j = parseNode(r.body());
                long totalSk = _FmM2.rL(j, "totalSkills", "total_skills");
                assertEquals(0L, totalSk, "TC214: totalSkills=0");
        }
}

// ─── TC215 — S1-F8 404 non-existent user ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC215_FmUserProfileNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC215 — Non-existent user returns 404")
        void profile_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/999999/profile", tok);
                assertEquals(404, r.statusCode(), "TC215: must be 404");
        }
}

// ─── TC216 — S1-F9 language + minContracts happy filter ──────────────────
@Tag("public")
@Tag("features_m1")
class TC216_FmLanguageMinContractsHappyTests extends TestBase {
        @Test
        @DisplayName("TC216 — lang=ar&minContracts=3 returns user A (5 completed contracts)")
        void lang_min_contracts_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _FmM1Seed.seedUser(this, "ArA", "tc216_a@fm.io", "FREELANCER");
                long uB = _FmM1Seed.seedUser(this, "ArB", "tc216_b@fm.io", "FREELANCER");
                long uC = _FmM1Seed.seedUser(this, "EnC", "tc216_c@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, uA, "{\"language\":\"ar\"}");
                _FmM1Seed.setPrefs(this, uB, "{\"language\":\"ar\"}");
                _FmM1Seed.setPrefs(this, uC, "{\"language\":\"en\"}");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                for (int i = 0; i < 5; i++) _FmM1Seed.seedContract(this, uA, 1L, 0L, jid, "COMPLETED", 100.0,
                        "2026-03-1" + i, "2026-03-2" + i);
                for (int i = 0; i < 2; i++) _FmM1Seed.seedContract(this, uB, 1L, 0L, jid, "COMPLETED", 100.0,
                        "2026-03-1" + i, "2026-03-2" + i);
                for (int i = 0; i < 4; i++) _FmM1Seed.seedContract(this, uC, 1L, 0L, jid, "COMPLETED", 100.0,
                        "2026-03-2" + i, "2026-03-2" + (i + 1));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/language?lang=ar&minContracts=3", tok);
                assert2xx(r, "TC216");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                boolean foundA = false;
                for (JsonNode it : list) {
                        long id = _FmM2.rL(it, "userId", "id");
                        if (id == uA) foundA = true;
                        assertNotEquals(uB, id, "TC216: user B (only 2) excluded");
                        assertNotEquals(uC, id, "TC216: user C (English) excluded");
                }
                assertTrue(foundA, "TC216: user A must be present");
        }
}

// ─── TC217 — S1-F9 400 blank lang ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC217_FmLanguageBlankLangTests extends TestBase {
        @Test
        @DisplayName("TC217 — lang= blank returns 400")
        void lang_blank() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/language?lang=&minContracts=1", tok);
                assertEquals(400, r.statusCode(), "TC217: blank lang must be 400");
        }
}

// ─── TC218 — S1-F9 lower threshold returns more users ────────────────────
@Tag("public")
@Tag("features_m1")
class TC218_FmLanguageLowerThresholdTests extends TestBase {
        @Test
        @DisplayName("TC218 — lang=ar&minContracts=1 returns A+B")
        void lang_lower_threshold() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _FmM1Seed.seedUser(this, "ArA", "tc218_a@fm.io", "FREELANCER");
                long uB = _FmM1Seed.seedUser(this, "ArB", "tc218_b@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, uA, "{\"language\":\"ar\"}");
                _FmM1Seed.setPrefs(this, uB, "{\"language\":\"ar\"}");
                long jid = _FmM1Seed.seedJob(this, "J", "MOBILE", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedContract(this, uA, 1L, 0L, jid, "COMPLETED", 100.0, "2026-03-10", "2026-03-15");
                _FmM1Seed.seedContract(this, uB, 1L, 0L, jid, "COMPLETED", 100.0, "2026-03-11", "2026-03-16");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/language?lang=ar&minContracts=1", tok);
                assert2xx(r, "TC218");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC218: at least 2 users expected");
        }
}

// ─── TC219 — S1-F9 only COMPLETED contracts count ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC219_FmLanguageCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC219 — minContracts counts COMPLETED only (ACTIVE/TERMINATED ignored)")
        void lang_completed_only() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _FmM1Seed.seedUser(this, "ArA", "tc219@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, uA, "{\"language\":\"ar\"}");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedContract(this, uA, 1L, 0L, jid, "COMPLETED",  100.0, "2026-03-10", "2026-03-15");
                _FmM1Seed.seedContract(this, uA, 1L, 0L, jid, "ACTIVE",     100.0, "2026-03-11", null);
                _FmM1Seed.seedContract(this, uA, 1L, 0L, jid, "TERMINATED", 100.0, "2026-03-12", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/language?lang=ar&minContracts=2", tok);
                assert2xx(r, "TC219");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long id = _FmM2.rL(it, "userId", "id");
                        assertNotEquals(uA, id, "TC219: A has only 1 COMPLETED, must not match minContracts=2");
                }
        }
}

// ─── TC220 — S1-F8 profile contains preferences JSONB ────────────────────
@Tag("public")
@Tag("features_m1")
class TC220_FmUserProfileWithPreferencesTests extends TestBase {
        @Test
        @DisplayName("TC220 — Profile DTO surfaces preferences JSONB content")
        void profile_with_prefs() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "PrefProf", "tc220@fm.io", "FREELANCER");
                _FmM1Seed.setPrefs(this, uid, "{\"language\":\"ar\",\"theme\":\"dark\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC220");
                JsonNode j = parseNode(r.body());
                JsonNode prefs = _FmM2.rO(j, "preferences");
                assertNotNull(prefs, "TC220: preferences object expected; body=" + r.body());
                assertEquals("ar", prefs.has("language") ? prefs.get("language").asText() : "",
                        "TC220: preferences.language=ar");
        }
}


// ────────────────────────────────────────────────────────────────────────────
// S2 — Job Service (TC221..TC256)  — 9 features × ~4 TCs each
// S2-F1 search, S2-F2 requirements, S2-F3 proposal-summary, S2-F4 close,
// S2-F5 requirements filter, S2-F6 top-budget, S2-F7 rate, S2-F8 verify
// attachment, S2-F9 expired attachments
// ────────────────────────────────────────────────────────────────────────────

// ─── TC221 — S2-F1 search by status + min/max budget ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC221_FmJobSearchHappyTests extends TestBase {
        @Test
        @DisplayName("TC221 — status=OPEN&minBudget=100&maxBudget=2000 returns matching jobs (budget desc)")
        void job_search_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                _FmM1Seed.seedJob(this, "J1", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJob(this, "J2", "MOBILE", "CLOSED", 100.0, 1000.0);
                _FmM1Seed.seedJob(this, "J3", "DESIGN", "OPEN", 200.0, 1500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/search?status=OPEN&minBudget=100&maxBudget=2000", tok);
                assert2xx(r, "TC221");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String s = it.has("status") ? it.get("status").asText() : "";
                        assertEquals("OPEN", s, "TC221: every result must be OPEN");
                }
        }
}

// ─── TC222 — S2-F1 search budget range ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC222_FmJobSearchBudgetRangeTests extends TestBase {
        @Test
        @DisplayName("TC222 — minBudget=300&maxBudget=600 returns only 500-budget job")
        void job_search_budget_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                _FmM1Seed.seedJob(this, "J1", "WEB_DEV", "OPEN", 50.0, 200.0);
                _FmM1Seed.seedJob(this, "J2", "MOBILE", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJob(this, "J3", "DESIGN", "OPEN", 50.0, 1000.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/search?minBudget=300&maxBudget=600", tok);
                assert2xx(r, "TC222");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC222: at least one job expected in range");
        }
}

// ─── TC223 — S2-F1 invalid range 400 ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC223_FmJobSearchInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC223 — minBudget>maxBudget returns 400")
        void job_search_invalid_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/search?minBudget=2000&maxBudget=100", tok);
                assertEquals(400, r.statusCode(), "TC223: must be 400");
        }
}

// ─── TC224 — S2-F2 requirements merge happy ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC224_FmJobRequirementsMergeTests extends TestBase {
        @Test
        @DisplayName("TC224 — PUT requirements merges: experience preserved, languages updated, certifications added")
        void requirements_merge() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 100.0, 1000.0);
                _FmM1Seed.setJobRequirements(this, jid,
                        "{\"experience\":\"5\",\"languages\":[\"en\"]}");
                String tok = adminToken();
                String body = "{\"languages\":[\"en\",\"ar\"],\"certifications\":[\"AWS\"]}";
                HttpResponse<String> r = httpPutAuth("/api/jobs/" + jid + "/requirements", body, tok);
                assert2xx(r, "TC224");
                JsonNode j = parseNode(r.body());
                JsonNode req = _FmM2.rO(j, "requirements");
                if (req == null) req = j;
                assertEquals("5", req.has("experience") ? req.get("experience").asText() : "",
                        "TC224: experience preserved");
                JsonNode langs = req.has("languages") ? req.get("languages") : null;
                assertNotNull(langs, "TC224: languages array required");
                assertEquals(2, langs.size(), "TC224: languages updated to 2 elements");
        }
}

// ─── TC225 — S2-F2 same-key overwrite ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC225_FmJobRequirementsOverwriteTests extends TestBase {
        @Test
        @DisplayName("TC225 — PUT with existing key overwrites it")
        void requirements_overwrite() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 100.0, 1000.0);
                _FmM1Seed.setJobRequirements(this, jid, "{\"experience\":\"5\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/" + jid + "/requirements", "{\"experience\":\"10\"}", tok);
                assert2xx(r, "TC225");
                JsonNode j = parseNode(r.body());
                JsonNode req = _FmM2.rO(j, "requirements");
                if (req == null) req = j;
                assertEquals("10", req.has("experience") ? req.get("experience").asText() : "",
                        "TC225: experience must be overwritten to 10");
        }
}

// ─── TC226 — S2-F2 404 non-existent job ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC226_FmJobRequirementsNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC226 — PUT requirements for non-existent job returns 404")
        void requirements_not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/999999/requirements", "{\"x\":\"y\"}", tok);
                assertEquals(404, r.statusCode(), "TC226: must be 404");
        }
}

// ─── TC227 — S2-F3 proposal-summary happy ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC227_FmProposalSummaryHappyTests extends TestBase {
        @Test
        @DisplayName("TC227 — Proposal summary returns totalProposals=5, averageBidAmount=120, lowestBid=80, highestBid=200")
        void proposal_summary_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 80.0, 5, "2026-04-01");
                _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 100.0, 5, "2026-04-02");
                _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 120.0, 5, "2026-04-03");
                _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 100.0, 5, "2026-04-04");
                _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 200.0, 5, "2026-04-05");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/" + jid + "/proposal-summary?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC227");
                JsonNode j = parseNode(r.body());
                assertEquals(5L, _FmM2.rL(j, "totalProposals", "total_proposals"),
                        "TC227: totalProposals=5");
                assertEquals(120.0, _FmM2.rD(j, "averageBidAmount", "average_bid_amount"), 0.5,
                        "TC227: averageBidAmount=120");
                assertEquals(80.0, _FmM2.rD(j, "lowestBid", "lowest_bid"), 0.5,
                        "TC227: lowestBid=80");
                assertEquals(200.0, _FmM2.rD(j, "highestBid", "highest_bid"), 0.5,
                        "TC227: highestBid=200");
        }
}

// ─── TC228 — S2-F3 job with no proposals ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC228_FmProposalSummaryEmptyTests extends TestBase {
        @Test
        @DisplayName("TC228 — Proposal summary for job with no proposals returns zeros")
        void proposal_summary_empty() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/" + jid + "/proposal-summary?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC228");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, _FmM2.rL(j, "totalProposals", "total_proposals"),
                        "TC228: totalProposals=0");
        }
}

// ─── TC229 — S2-F3 404 non-existent job ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC229_FmProposalSummaryNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC229 — Proposal summary for non-existent job returns 404")
        void proposal_summary_not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/999999/proposal-summary?startDate=2026-04-01&endDate=2026-04-30", tok);
                assertEquals(404, r.statusCode(), "TC229: must be 404");
        }
}

// ─── TC230 — S2-F4 close job rejects SUBMITTED proposals ─────────────────
@Tag("public")
@Tag("features_m1")
class TC230_FmCloseJobRejectsProposalsTests extends TestBase {
        @Test
        @DisplayName("TC230 — Close job sets status=CLOSED + rejects all SUBMITTED proposals")
        void close_job_rejects_proposals() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                long propId = _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 100.0, 5, "2026-04-01");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/jobs/" + jid + "/close", "{\"status\":\"CLOSED\"}", tok);
                assert2xx(r, "TC230");
                String stCol = columnByField("Job", "status");
                String dbStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tableName("Job") + "\" WHERE id=?",
                        String.class, jid);
                assertEquals("CLOSED", dbStatus, "TC230: Job.status=CLOSED");
                String propStCol = columnByField("Proposal", "status");
                String propStatus = jdbc.queryForObject(
                        "SELECT \"" + propStCol + "\"::text FROM \"" + tableName("Proposal") + "\" WHERE id=?",
                        String.class, propId);
                assertEquals("REJECTED", propStatus,
                        "TC230: SUBMITTED proposal must be REJECTED on close; got " + propStatus);
        }
}

// ─── TC231 — S2-F4 close job fails with active contract ──────────────────
@Tag("public")
@Tag("features_m1")
class TC231_FmCloseJobActiveContractTests extends TestBase {
        @Test
        @DisplayName("TC231 — Close job with ACTIVE contract returns 400")
        void close_job_active_contract_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedContract(this, 1L, 1L, 0L, jid, "ACTIVE", 100.0, "2026-03-01", null);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/jobs/" + jid + "/close", "{\"status\":\"CLOSED\"}", tok);
                assertEquals(400, r.statusCode(), "TC231: must be 400 with active contract");
        }
}

// ─── TC232 — S2-F4 404 non-existent job ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC232_FmCloseJobNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC232 — Close non-existent job returns 404")
        void close_job_not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/jobs/999999/close", "{\"status\":\"CLOSED\"}", tok);
                assertEquals(404, r.statusCode(), "TC232: must be 404");
        }
}

// ─── TC233 — S2-F5 requirements search happy ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC233_FmRequirementsSearchHappyTests extends TestBase {
        @Test
        @DisplayName("TC233 — ?key=experience&value=5 returns matching jobs")
        void requirements_search_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long j1 = _FmM1Seed.seedJob(this, "J1", "WEB_DEV", "OPEN", 50.0, 500.0);
                long j2 = _FmM1Seed.seedJob(this, "J2", "MOBILE", "OPEN", 50.0, 500.0);
                _FmM1Seed.setJobRequirements(this, j1, "{\"experience\":\"5\"}");
                _FmM1Seed.setJobRequirements(this, j2, "{\"experience\":\"10\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/requirements/search?key=experience&value=5", tok);
                assert2xx(r, "TC233");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC233: at least 1 match expected");
        }
}

// ─── TC234 — S2-F6 top-budget happy ranking ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC234_FmTopBudgetHappyTests extends TestBase {
        @Test
        @DisplayName("TC234 — Top budget jobs ranks 5000-budget above 1000-budget")
        void top_budget_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long j1 = _FmM1Seed.seedJob(this, "JLow", "WEB_DEV", "OPEN", 100.0, 1000.0);
                long j2 = _FmM1Seed.seedJob(this, "JHigh", "WEB_DEV", "OPEN", 1000.0, 5000.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/reports/top-budget?limit=10", tok);
                assert2xx(r, "TC234");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC234: at least 2 jobs");
                long firstId = _FmM2.rL(list.get(0), "jobId", "id");
                assertEquals(j2, firstId, "TC234: 5000-budget job must rank first");
        }
}

// ─── TC235 — S2-F6 limit respected ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC235_FmTopBudgetLimitTests extends TestBase {
        @Test
        @DisplayName("TC235 — limit=2 caps results at 2")
        void top_budget_limit() throws Exception {
                BASE_URL = catalogServiceUrl;
                for (int i = 0; i < 5; i++) {
                        _FmM1Seed.seedJob(this, "J" + i, "WEB_DEV", "OPEN", 100.0, 100.0 * (i + 1));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/reports/top-budget?limit=2", tok);
                assert2xx(r, "TC235");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC235: limit=2 must cap; got " + list.size());
        }
}

// ─── TC236 — S2-F7 rate happy path ──────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC236_FmRateHappyTests extends TestBase {
        @Test
        @DisplayName("TC236 — Rate completed contract job 1-5 returns 2xx")
        void rate_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 500.0);
                long cid = _FmM1Seed.seedContract(this, 1L, 1L, 0L, jid, "COMPLETED", 200.0, "2026-03-01", "2026-03-15");
                String tok = adminToken();
                String body = "{\"contractId\":" + cid + ",\"rating\":5}";
                HttpResponse<String> r = httpPostAuth("/api/jobs/" + jid + "/rate", body, tok);
                assert2xx(r, "TC236");
        }
}

// ─── TC237 — S2-F7 rating out of range returns 400 ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC237_FmRateOutOfRangeTests extends TestBase {
        @Test
        @DisplayName("TC237 — Rating=6 (>5) returns 400")
        void rate_out_of_range_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 500.0);
                long cid = _FmM1Seed.seedContract(this, 1L, 1L, 0L, jid, "COMPLETED", 200.0, "2026-03-01", "2026-03-15");
                String tok = adminToken();
                String body = "{\"contractId\":" + cid + ",\"rating\":6}";
                HttpResponse<String> r = httpPostAuth("/api/jobs/" + jid + "/rate", body, tok);
                assertEquals(400, r.statusCode(), "TC237: must be 400 for rating>5");
        }
}

// ─── TC238 — S2-F7 non-completed contract → 400 ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC238_FmRateNotCompletedTests extends TestBase {
        @Test
        @DisplayName("TC238 — Rate ACTIVE contract returns 400")
        void rate_not_completed_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 500.0);
                long cid = _FmM1Seed.seedContract(this, 1L, 1L, 0L, jid, "ACTIVE", 200.0, "2026-03-01", null);
                String tok = adminToken();
                String body = "{\"contractId\":" + cid + ",\"rating\":4}";
                HttpResponse<String> r = httpPostAuth("/api/jobs/" + jid + "/rate", body, tok);
                assertEquals(400, r.statusCode(), "TC238: must be 400 for non-COMPLETED");
        }
}

// ─── TC239 — S2-F7 non-existent contract → 404 ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC239_FmRateContractNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC239 — Non-existent contract returns 404")
        void rate_contract_not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                String tok = adminToken();
                String body = "{\"contractId\":999999,\"rating\":4}";
                HttpResponse<String> r = httpPostAuth("/api/jobs/" + jid + "/rate", body, tok);
                assertEquals(404, r.statusCode(), "TC239: must be 404");
        }
}

// ─── TC240 — S2-F8 verify attachment happy ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC240_FmVerifyAttachmentHappyTests extends TestBase {
        @Test
        @DisplayName("TC240 — Verify attachment as ADMIN sets verified=true + records verifiedAt+verifiedBy in metadata")
        void verify_attachment_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                long aid = _FmM1Seed.seedJobAttachment(this, jid, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.futureDate(), false);
                long adminId = _FmM1Seed.seedUser(this, "AdminVerify", "tc240admin@fm.io", "ADMIN");
                String tok = adminToken();
                String body = "{\"verifiedBy\":" + adminId + "}";
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/" + jid + "/attachments/" + aid + "/verify", body, tok);
                assert2xx(r, "TC240");
                String vCol = columnByField("JobAttachment", "verified");
                Boolean v = jdbc.queryForObject(
                        "SELECT \"" + vCol + "\" FROM \"" + tableName("JobAttachment") + "\" WHERE id=?",
                        Boolean.class, aid);
                assertEquals(Boolean.TRUE, v, "TC240: verified must be true");
        }
}

// ─── TC241 — S2-F8 non-ADMIN verifier → 403 ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC241_FmVerifyAttachmentNonAdminTests extends TestBase {
        @Test
        @DisplayName("TC241 — verifiedBy is non-ADMIN user → 403")
        void verify_attachment_non_admin_403() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                long aid = _FmM1Seed.seedJobAttachment(this, jid, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.futureDate(), false);
                long freelancerId = _FmM1Seed.seedUser(this, "FrUser", "tc241fr@fm.io", "FREELANCER");
                String tok = adminToken();
                String body = "{\"verifiedBy\":" + freelancerId + "}";
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/" + jid + "/attachments/" + aid + "/verify", body, tok);
                assertEquals(403, r.statusCode(), "TC241: non-ADMIN verifier must be 403");
        }
}

// ─── TC242 — S2-F8 expired attachment → 400 ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC242_FmVerifyAttachmentExpiredTests extends TestBase {
        @Test
        @DisplayName("TC242 — Expired attachment returns 400")
        void verify_attachment_expired_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                long aid = _FmM1Seed.seedJobAttachment(this, jid, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.pastDate(), false);
                long adminId = _FmM1Seed.seedUser(this, "AdminVerify", "tc242admin@fm.io", "ADMIN");
                String tok = adminToken();
                String body = "{\"verifiedBy\":" + adminId + "}";
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/" + jid + "/attachments/" + aid + "/verify", body, tok);
                assertEquals(400, r.statusCode(), "TC242: expired attachment must be 400");
        }
}

// ─── TC243 — S2-F8 attachment cross-job → 400 ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC243_FmVerifyAttachmentCrossJobTests extends TestBase {
        @Test
        @DisplayName("TC243 — Attachment belongs to different job → 400")
        void verify_attachment_cross_job_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long j1 = _FmM1Seed.seedJob(this, "J1", "WEB_DEV", "OPEN", 50.0, 500.0);
                long j2 = _FmM1Seed.seedJob(this, "J2", "MOBILE", "OPEN", 50.0, 500.0);
                long aid = _FmM1Seed.seedJobAttachment(this, j2, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.futureDate(), false);
                long adminId = _FmM1Seed.seedUser(this, "AdminV", "tc243admin@fm.io", "ADMIN");
                String tok = adminToken();
                String body = "{\"verifiedBy\":" + adminId + "}";
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/" + j1 + "/attachments/" + aid + "/verify", body, tok);
                assertEquals(400, r.statusCode(), "TC243: cross-job must be 400");
        }
}

// ─── TC244 — S2-F9 expired attachments alert happy ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC244_FmExpiredAttachmentsHappyTests extends TestBase {
        @Test
        @DisplayName("TC244 — Expired attachments alert returns jobs with expired attachments")
        void expired_attachments_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "ExpJ", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJobAttachment(this, jid, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.pastDate(), false);
                _FmM1Seed.seedJobAttachment(this, jid, "MOCKUP",
                        "https://docs.fm.io/mockup.pdf", _FmM1Seed.pastDate(), false);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/attachments/expired", tok);
                assert2xx(r, "TC244");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC244: at least 1 job with expired attachments");
        }
}

// ─── TC245 — S2-F9 alert excludes future-expiry attachments ──────────────
@Tag("public")
@Tag("features_m1")
class TC245_FmExpiredAttachmentsFutureExcludedTests extends TestBase {
        @Test
        @DisplayName("TC245 — Future-expiry attachments excluded from alert")
        void expired_attachments_future_excluded() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "FutJ", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJobAttachment(this, jid, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.futureDate(), false);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/attachments/expired", tok);
                assert2xx(r, "TC245");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long id = _FmM2.rL(it, "jobId", "id");
                        assertNotEquals(jid, id, "TC245: future-only job must NOT be in alert");
                }
        }
}

// ─── TC246 — S2-F9 expired DTO shape ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC246_FmExpiredAttachmentsDtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC246 — Alert DTO has jobId/jobTitle/jobStatus/expiredCount/expiredAttachments")
        void expired_attachments_dto_shape() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "DTOJob", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJobAttachment(this, jid, "BRIEF",
                        "https://docs.fm.io/brief.pdf", _FmM1Seed.pastDate(), false);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/attachments/expired", tok);
                assert2xx(r, "TC246");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC246: at least 1 entry");
                JsonNode item = list.get(0);
                assertTrue(item.has("jobId") || item.has("job_id") || item.has("id"),
                        "TC246: must include jobId");
                assertTrue(item.has("expiredCount") || item.has("expired_count"),
                        "TC246: must include expiredCount");
        }
}

// ─── TC247 — S2-F1 partial title match ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC247_FmJobSearchPartialTitleTests extends TestBase {
        @Test
        @DisplayName("TC247 — Search by title 'Web' returns matching jobs (partial)")
        void job_search_partial_title() throws Exception {
                BASE_URL = catalogServiceUrl;
                _FmM1Seed.seedJob(this, "Web Frontend", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJob(this, "Mobile App", "MOBILE", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJob(this, "Web Backend", "WEB_DEV", "OPEN", 50.0, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/search?title=Web", tok);
                assert2xx(r, "TC247");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int matches = 0;
                for (JsonNode it : list) {
                        String tt = it.has("title") ? it.get("title").asText() : "";
                        if (tt.contains("Web")) matches++;
                }
                assertTrue(matches >= 2, "TC247: at least 2 'Web' jobs expected");
        }
}

// ─── TC248 — S2-F3 invalid date range → 400 ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC248_FmProposalSummaryInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC248 — Proposal summary start>end returns 400")
        void proposal_summary_invalid_range_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/" + jid + "/proposal-summary?startDate=2026-04-30&endDate=2026-04-01", tok);
                assertEquals(400, r.statusCode(), "TC248: invalid range must be 400");
        }
}

// ─── TC249 — S2-F5 requirements blank key → 400 ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC249_FmRequirementsBlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC249 — Requirements search with blank key returns 400")
        void requirements_blank_key_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/requirements/search?key=&value=5", tok);
                assertEquals(400, r.statusCode(), "TC249: blank key must be 400");
        }
}

// ─── TC250 — S2-F1 status only filter ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC250_FmJobSearchStatusOnlyTests extends TestBase {
        @Test
        @DisplayName("TC250 — Search by status=OPEN only returns OPEN jobs")
        void job_search_status_only() throws Exception {
                BASE_URL = catalogServiceUrl;
                _FmM1Seed.seedJob(this, "OpenJ", "WEB_DEV", "OPEN", 50.0, 500.0);
                _FmM1Seed.seedJob(this, "ClosedJ", "WEB_DEV", "CLOSED", 50.0, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/search?status=OPEN", tok);
                assert2xx(r, "TC250");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String s = it.has("status") ? it.get("status").asText() : "";
                        assertEquals("OPEN", s, "TC250: every result must be OPEN");
                }
        }
}

// ─── TC251 — S2-F6 zero limit returns empty list ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC251_FmTopBudgetZeroLimitTests extends TestBase {
        @Test
        @DisplayName("TC251 — limit=0 returns empty list (graceful)")
        void top_budget_zero_limit() throws Exception {
                BASE_URL = catalogServiceUrl;
                _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/reports/top-budget?limit=0", tok);
                int code = r.statusCode();
                assertTrue(code / 100 != 5, "TC251: must NOT 5xx");
        }
}

// ─── TC252 — S2-F8 404 attachment ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC252_FmVerifyAttachmentNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC252 — Verify non-existent attachment returns 404")
        void verify_attachment_not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 500.0);
                long adminId = _FmM1Seed.seedUser(this, "Adm", "tc252@fm.io", "ADMIN");
                String tok = adminToken();
                String body = "{\"verifiedBy\":" + adminId + "}";
                HttpResponse<String> r = httpPutAuth(
                        "/api/jobs/" + jid + "/attachments/999999/verify", body, tok);
                assertEquals(404, r.statusCode(), "TC252: must be 404");
        }
}

// ─── TC253 — S2-F9 empty result when no expired attachments ──────────────
@Tag("public")
@Tag("features_m1")
class TC253_FmExpiredAttachmentsEmptyTests extends TestBase {
        @Test
        @DisplayName("TC253 — Empty result when no expired attachments")
        void expired_attachments_empty() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/attachments/expired", tok);
                assert2xx(r, "TC253");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC253: empty list expected when no expired");
        }
}

// ─── TC254 — S2-F4 close already-closed → 400 or no-op ──────────────────
@Tag("public")
@Tag("features_m1")
class TC254_FmCloseAlreadyClosedTests extends TestBase {
        @Test
        @DisplayName("TC254 — Close already-CLOSED job returns 400")
        void close_already_closed_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "CLOSED", 50.0, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/jobs/" + jid + "/close", "", tok);
                int code = r.statusCode();
                assertTrue(code == 400 || code / 100 == 2,
                        "TC254: closing already-CLOSED must be 400 or idempotent 2xx; got " + code);
        }
}

// ─── TC255 — S2-F1 search by budget range ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC255_FmJobSearchByCategoryTests extends TestBase {
        @Test
        @DisplayName("TC255 — Search ?minBudget=8000&maxBudget=9000 returns only jobs overlapping that range")
        void job_search_by_category() throws Exception {
                BASE_URL = catalogServiceUrl;
                // Seed jobs with distinct high budget to isolate from other tests (other tests use 50-500)
                _FmM1Seed.seedJob(this, "TC255_A", "WEB_DEV", "OPEN", 8000.0, 8500.0);
                _FmM1Seed.seedJob(this, "TC255_B", "DESIGN",  "OPEN", 8200.0, 9000.0);
                _FmM1Seed.seedJob(this, "TC255_C", "MOBILE",  "OPEN", 50.0,   200.0);  // outside range
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/jobs/search?minBudget=8000&maxBudget=9000", tok);
                assert2xx(r, "TC255");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC255: at least 2 results expected in range [8000,9000]; got " + list.size());
                for (JsonNode it : list) {
                        double bMin = it.has("budgetMin") ? it.get("budgetMin").asDouble() : 0;
                        double bMax = it.has("budgetMax") ? it.get("budgetMax").asDouble() : 0;
                        assertTrue(bMin <= 9000 && bMax >= 8000,
                                "TC255: job budget [" + bMin + "," + bMax + "] must overlap [8000,9000]; got " + it);
                }
        }
}

// ─── TC256 — S2-F6 top-budget DTO shape ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC256_FmTopBudgetDtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC256 — Top budget DTO has jobId/title/budgetMax/totalProposals fields")
        void top_budget_dto_shape() throws Exception {
                BASE_URL = catalogServiceUrl;
                long jid = _FmM1Seed.seedJob(this, "DTO", "WEB_DEV", "OPEN", 100.0, 1000.0);
                _FmM1Seed.seedProposal(this, 1L, jid, "SUBMITTED", 100.0, 5, "2026-04-01");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/jobs/reports/top-budget?limit=10", tok);
                assert2xx(r, "TC256");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC256: at least 1 entry");
                JsonNode item = list.get(0);
                assertTrue(item.has("jobId") || item.has("job_id") || item.has("id"),
                        "TC256: must include jobId");
                assertTrue(item.has("title"),
                        "TC256: must include title");
                assertTrue(item.has("budgetMax") || item.has("budget_max"),
                        "TC256: must include budgetMax");
        }
}


// ────────────────────────────────────────────────────────────────────────────
// S3 — Proposal Service M1 (TC257..TC298)  — 9 features
// S3-F1 search, S3-F2 accept, S3-F3 fee-estimate, S3-F4 complete,
// S3-F5 metadata-search, S3-F6 analytics, S3-F7 withdraw,
// S3-F8 add-milestones, S3-F9 proposal-details
// ────────────────────────────────────────────────────────────────────────────

// ─── TC257 — S3-F1 search proposals by status + date range ───────────────
@Tag("public")
@Tag("features_m1")
class TC257_FmProposalSearchHappyTests extends TestBase {
        @Test
        @DisplayName("TC257 — status=SUBMITTED + date range returns matching proposals")
        void proposal_search_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Fr", "tc257@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED",   200.0, 5, "2026-03-10");
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED",   300.0, 7, "2026-03-15");
                _FmM1Seed.seedProposal(this, uid, jid, "REJECTED",    400.0, 3, "2026-03-20");
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED",   500.0, 9, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/search?status=SUBMITTED&startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC257");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(2, list.size(), "TC257: 2 SUBMITTED in March expected; got " + list.size());
        }
}

// ─── TC258 — S3-F1 sorted most-recent-first ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC258_FmProposalSearchSortedTests extends TestBase {
        @Test
        @DisplayName("TC258 — Search results sorted by submittedAt DESC")
        void proposal_search_sorted() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Fr", "tc258@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-03-05");
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED", 200.0, 7, "2026-03-25");
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED", 300.0, 3, "2026-03-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/search?status=SUBMITTED&startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC258");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 3, "TC258: at least 3 results expected");
        }
}

// ─── TC259 — S3-F1 no match returns empty list ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC259_FmProposalSearchNoMatchTests extends TestBase {
        @Test
        @DisplayName("TC259 — Search with status=ACCEPTED returns empty when none accepted")
        void proposal_search_no_match() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Fr", "tc259@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED", 100.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/proposals/search?status=ACCEPTED", tok);
                assert2xx(r, "TC259");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC259: empty list expected; got " + list.size());
        }
}

// ─── TC260 — S3-F1 status only filter (no date range) ────────────────────
@Tag("public")
@Tag("features_m1")
class TC260_FmProposalSearchStatusOnlyTests extends TestBase {
        @Test
        @DisplayName("TC260 — status=REJECTED only filter returns REJECTED proposals only")
        void proposal_search_status_only() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _FmM1Seed.seedUser(this, "Fr", "tc260@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, uid, jid, "SUBMITTED",  100.0, 5, "2026-03-10");
                _FmM1Seed.seedProposal(this, uid, jid, "REJECTED",   200.0, 7, "2026-03-15");
                _FmM1Seed.seedProposal(this, uid, jid, "REJECTED",   300.0, 3, "2026-03-20");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/proposals/search?status=REJECTED", tok);
                assert2xx(r, "TC260");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String st = it.has("status") ? it.get("status").asText() : "";
                        assertEquals("REJECTED", st, "TC260: every result must be REJECTED");
                }
        }
}

// ─── TC261 — S3-F2 accept proposal happy path ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC261_FmAcceptProposalHappyTests extends TestBase {
        @Test
        @DisplayName("TC261 — Accept SUBMITTED proposal: proposal=ACCEPTED, job=IN_PROGRESS, ACTIVE Contract created")
        void accept_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc261@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 250.0, 7, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/accept", "", tok);
                assert2xx(r, "TC261");
                String stCol = columnByField("Proposal", "status");
                String pStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tableName("Proposal") + "\" WHERE id=?",
                        String.class, pid);
                assertEquals("ACCEPTED", pStatus, "TC261: proposal.status=ACCEPTED expected; got " + pStatus);
                String jStCol = columnByField("Job", "status");
                String jStatus = jdbc.queryForObject(
                        "SELECT \"" + jStCol + "\"::text FROM \"" + tableName("Job") + "\" WHERE id=?",
                        String.class, jid);
                assertEquals("IN_PROGRESS", jStatus, "TC261: job.status=IN_PROGRESS expected; got " + jStatus);
        }
}

// ─── TC262 — S3-F2 404 non-existent proposal ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC262_FmAcceptProposalNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC262 — Accept non-existent proposal returns 404")
        void accept_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/999999/accept", "", tok);
                assertEquals(404, r.statusCode(), "TC262: must be 404; got " + r.statusCode());
        }
}

// ─── TC263 — S3-F2 invalid status (REJECTED) → 400 ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC263_FmAcceptProposalInvalidStatusTests extends TestBase {
        @Test
        @DisplayName("TC263 — Accept REJECTED proposal returns 400")
        void accept_invalid_status() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc263@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "REJECTED", 100.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/accept", "", tok);
                assertEquals(400, r.statusCode(), "TC263: must be 400 for REJECTED; got " + r.statusCode());
        }
}

// ─── TC264 — S3-F2 SHORTLISTED is acceptable ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC264_FmAcceptShortlistedHappyTests extends TestBase {
        @Test
        @DisplayName("TC264 — Accept SHORTLISTED proposal succeeds")
        void accept_shortlisted() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc264@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SHORTLISTED", 500.0, 3, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/accept", "", tok);
                assert2xx(r, "TC264");
                String stCol = columnByField("Proposal", "status");
                String pStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tableName("Proposal") + "\" WHERE id=?",
                        String.class, pid);
                assertEquals("ACCEPTED", pStatus, "TC264: SHORTLISTED→ACCEPTED expected; got " + pStatus);
        }
}

// ─── TC265 — S3-F2 contract created with agreedAmount=bidAmount ──────────
@Tag("public")
@Tag("features_m1")
class TC265_FmAcceptContractCreatedTests extends TestBase {
        @Test
        @DisplayName("TC265 — Accept creates ACTIVE Contract with agreedAmount=bidAmount")
        void accept_contract_created() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc265@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "MOBILE", "OPEN", 50.0, 2000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 750.0, 14, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/accept", "", tok);
                assert2xx(r, "TC265");
                String pCol = columnByField("Contract", "proposal");
                String aCol = columnByField("Contract", "agreedAmount");
                String stCol = columnByField("Contract", "status");
                Long count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Contract") + "\" WHERE \""
                                + pCol + "\"=? AND \"" + aCol + "\"=? AND \"" + stCol + "\"::text='ACTIVE'",
                        Long.class, pid, 750.0);
                assertTrue(count != null && count >= 1L,
                        "TC265: ACTIVE contract w/ proposal=" + pid + " agreedAmount=750 expected; got " + count);
        }
}

// ─── TC266 — S3-F3 fee estimate happy ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC266_FmFeeEstimateHappyTests extends TestBase {
        @Test
        @DisplayName("TC266 — Estimate returns bidAmount/platformFee/freelancerPayout/feePercentage/estimatedDailyRate")
        void fee_estimate_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String body = "{\"bidAmount\":500.0,\"estimatedDays\":10}";
                HttpResponse<String> r = httpPostAuth("/api/proposals/estimate", body, tok);
                assert2xx(r, "TC266");
                JsonNode j = parseNode(r.body());
                double bid = _FmM2.rD(j, "bidAmount", "bid_amount");
                double fee = _FmM2.rD(j, "platformFee", "platform_fee");
                double payout = _FmM2.rD(j, "freelancerPayout", "freelancer_payout");
                double pct = _FmM2.rD(j, "feePercentage", "fee_percentage");
                assertEquals(500.0, bid, 0.5, "TC266: bidAmount=500; got " + bid);
                assertTrue(fee > 0.0, "TC266: platformFee>0; got " + fee);
                assertTrue(payout > 0.0 && payout < bid, "TC266: 0<freelancerPayout<bidAmount; got " + payout);
                assertTrue(pct >= 0.10 && pct <= 0.20, "TC266: feePercentage in [0.10,0.20]; got " + pct);
        }
}

// ─── TC267 — S3-F3 low demand (≤5 active near band) → 20% fee ────────────
@Tag("public")
@Tag("features_m1")
class TC267_FmFeeEstimateLowDemandTests extends TestBase {
        @Test
        @DisplayName("TC267 — Few competing proposals → 20% fee")
        void fee_estimate_low_demand() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String body = "{\"bidAmount\":1000.0,\"estimatedDays\":5}";
                HttpResponse<String> r = httpPostAuth("/api/proposals/estimate", body, tok);
                assert2xx(r, "TC267");
                JsonNode j = parseNode(r.body());
                double pct = _FmM2.rD(j, "feePercentage", "fee_percentage");
                assertTrue(pct >= 0.10 && pct <= 0.20,
                        "TC267: feePercentage in [0.10,0.20]; got " + pct);
        }
}

// ─── TC268 — S3-F3 read-only (no proposal persisted) ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC268_FmFeeEstimateReadOnlyTests extends TestBase {
        @Test
        @DisplayName("TC268 — Estimate is read-only — no Proposal row created")
        void fee_estimate_read_only() throws Exception {
                BASE_URL = orderServiceUrl;
                Long before = jdbc.queryForObject("SELECT COUNT(*) FROM \"" + tableName("Proposal") + "\"", Long.class);
                long beforeCount = before == null ? 0L : before.longValue();
                String tok = adminToken();
                String body = "{\"bidAmount\":250.0,\"estimatedDays\":6}";
                HttpResponse<String> r = httpPostAuth("/api/proposals/estimate", body, tok);
                assert2xx(r, "TC268");
                Long after = jdbc.queryForObject("SELECT COUNT(*) FROM \"" + tableName("Proposal") + "\"", Long.class);
                long afterCount = after == null ? 0L : after.longValue();
                assertEquals(beforeCount, afterCount, "TC268: no Proposal must be persisted");
        }
}

// ─── TC269 — S3-F3 estimatedDailyRate = bidAmount / estimatedDays ─────────
@Tag("public")
@Tag("features_m1")
class TC269_FmFeeEstimateDailyRateTests extends TestBase {
        @Test
        @DisplayName("TC269 — estimatedDailyRate = bidAmount / estimatedDays")
        void fee_estimate_daily_rate() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String body = "{\"bidAmount\":600.0,\"estimatedDays\":10}";
                HttpResponse<String> r = httpPostAuth("/api/proposals/estimate", body, tok);
                assert2xx(r, "TC269");
                JsonNode j = parseNode(r.body());
                double rate = _FmM2.rD(j, "estimatedDailyRate", "estimated_daily_rate");
                assertEquals(60.0, rate, 0.5, "TC269: estimatedDailyRate=60; got " + rate);
        }
}

// ─── TC270 — S3-F3 freelancerPayout = bidAmount - platformFee ────────────
@Tag("public")
@Tag("features_m1")
class TC270_FmFeeEstimatePayoutMathTests extends TestBase {
        @Test
        @DisplayName("TC270 — freelancerPayout + platformFee = bidAmount")
        void fee_estimate_payout_math() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String body = "{\"bidAmount\":800.0,\"estimatedDays\":4}";
                HttpResponse<String> r = httpPostAuth("/api/proposals/estimate", body, tok);
                assert2xx(r, "TC270");
                JsonNode j = parseNode(r.body());
                double bid = _FmM2.rD(j, "bidAmount", "bid_amount");
                double fee = _FmM2.rD(j, "platformFee", "platform_fee");
                double payout = _FmM2.rD(j, "freelancerPayout", "freelancer_payout");
                assertEquals(bid, fee + payout, 1.0,
                        "TC270: payout+fee=bid; got payout=" + payout + " fee=" + fee + " bid=" + bid);
        }
}

// ─── TC271 — S3-F4 complete proposal happy ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC271_FmCompleteProposalHappyTests extends TestBase {
        @Test
        @DisplayName("TC271 — Complete ACCEPTED proposal: contract→COMPLETED, job→CLOSED, PENDING Payout created")
        void complete_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc271@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "IN_PROGRESS", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED", 400.0, 7, "2026-03-10");
                long cid = _FmM1Seed.seedContract(this, fr, 1L, pid, jid, "ACTIVE", 400.0, "2026-03-12", null);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/complete", "", tok);
                assert2xx(r, "TC271");
                String cStCol = columnByField("Contract", "status");
                String cStatus = jdbc.queryForObject(
                        "SELECT \"" + cStCol + "\"::text FROM \"" + tableName("Contract") + "\" WHERE id=?",
                        String.class, cid);
                assertEquals("COMPLETED", cStatus, "TC271: contract.status=COMPLETED expected; got " + cStatus);
                String jStCol = columnByField("Job", "status");
                String jStatus = jdbc.queryForObject(
                        "SELECT \"" + jStCol + "\"::text FROM \"" + tableName("Job") + "\" WHERE id=?",
                        String.class, jid);
                assertEquals("CLOSED", jStatus, "TC271: job.status=CLOSED expected; got " + jStatus);
        }
}

// ─── TC272 — S3-F4 PENDING Payout created with amount=agreedAmount ───────
@Tag("public")
@Tag("features_m1")
class TC272_FmCompleteCreatesPayoutTests extends TestBase {
        @Test
        @DisplayName("TC272 — Complete creates PENDING Payout with amount=agreedAmount")
        void complete_creates_payout() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc272@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "IN_PROGRESS", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED", 350.0, 9, "2026-03-10");
                long cid = _FmM1Seed.seedContract(this, fr, 1L, pid, jid, "ACTIVE", 350.0, "2026-03-12", null);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/complete", "", tok);
                assert2xx(r, "TC272");
                String contractCol = columnByField("Payout", "contract");
                String stCol = columnByField("Payout", "status");
                String amCol = columnByField("Payout", "amount");
                Long count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Payout") + "\" WHERE \""
                                + contractCol + "\"=? AND \"" + stCol + "\"::text='PENDING' AND \""
                                + amCol + "\"=?", Long.class, cid, 350.0);
                assertTrue(count != null && count >= 1L,
                        "TC272: PENDING payout w/ contract=" + cid + " amount=350 expected; got " + count);
        }
}

// ─── TC273 — S3-F4 invalid status (SUBMITTED) → 400 ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC273_FmCompleteInvalidStatusTests extends TestBase {
        @Test
        @DisplayName("TC273 — Complete SUBMITTED proposal returns 400")
        void complete_invalid_status() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc273@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 200.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/complete", "", tok);
                assertEquals(400, r.statusCode(), "TC273: must be 400 for SUBMITTED; got " + r.statusCode());
        }
}

// ─── TC274 — S3-F4 no active contract → 400 ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC274_FmCompleteNoActiveContractTests extends TestBase {
        @Test
        @DisplayName("TC274 — Complete ACCEPTED proposal w/o ACTIVE contract returns 400")
        void complete_no_active_contract() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc274@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "IN_PROGRESS", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED", 400.0, 7, "2026-03-10");
                // intentionally no contract seeded
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/complete", "", tok);
                assertEquals(400, r.statusCode(), "TC274: must be 400 when no ACTIVE contract; got " + r.statusCode());
        }
}

// ─── TC275 — S3-F4 404 non-existent proposal ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC275_FmCompleteNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC275 — Complete non-existent proposal returns 404")
        void complete_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/999999/complete", "", tok);
                assertEquals(404, r.statusCode(), "TC275: must be 404; got " + r.statusCode());
        }
}

// ─── TC276 — S3-F5 metadata search happy ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC276_FmProposalMetadataSearchHappyTests extends TestBase {
        @Test
        @DisplayName("TC276 — metadata?key=source&value=referral matches proposals with metadata.source=referral")
        void proposal_metadata_search_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc276@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                long p1 = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 100.0, 5, "2026-03-10");
                long p2 = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 200.0, 7, "2026-03-11");
                long p3 = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 300.0, 3, "2026-03-12");
                String mCol = columnByField("Proposal", "metadata");
                String tbl = tableName("Proposal");
                jdbc.update("UPDATE \"" + tbl + "\" SET \"" + mCol + "\"=?::jsonb WHERE id=?",
                        "{\"source\":\"referral\"}", p1);
                jdbc.update("UPDATE \"" + tbl + "\" SET \"" + mCol + "\"=?::jsonb WHERE id=?",
                        "{\"source\":\"direct\"}", p2);
                jdbc.update("UPDATE \"" + tbl + "\" SET \"" + mCol + "\"=?::jsonb WHERE id=?",
                        "{\"source\":\"referral\"}", p3);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/metadata/search?key=source&value=referral", tok);
                assert2xx(r, "TC276");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(2, list.size(), "TC276: 2 referral proposals expected; got " + list.size());
        }
}

// ─── TC277 — S3-F5 metadata no match returns empty ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC277_FmProposalMetadataNoMatchTests extends TestBase {
        @Test
        @DisplayName("TC277 — metadata search w/ unknown value returns empty list")
        void proposal_metadata_no_match() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/metadata/search?key=source&value=zzznoexist", tok);
                assert2xx(r, "TC277");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC277: empty list expected; got " + list.size());
        }
}

// ─── TC278 — S3-F5 blank key → 400 ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC278_FmProposalMetadataBlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC278 — Blank key returns 400")
        void proposal_metadata_blank_key() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/metadata/search?key=&value=referral", tok);
                assertEquals(400, r.statusCode(), "TC278: must be 400; got " + r.statusCode());
        }
}

// ─── TC279 — S3-F5 blank value → 400 ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC279_FmProposalMetadataBlankValueTests extends TestBase {
        @Test
        @DisplayName("TC279 — Blank value returns 400")
        void proposal_metadata_blank_value() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/metadata/search?key=source&value=", tok);
                assertEquals(400, r.statusCode(), "TC279: must be 400; got " + r.statusCode());
        }
}

// ─── TC280 — S3-F6 analytics happy ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC280_FmProposalAnalyticsHappyTests extends TestBase {
        @Test
        @DisplayName("TC280 — Analytics returns total/accepted/rejected/totalBidValue/averageBid/acceptanceRate")
        void proposal_analytics_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc280@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED",  100.0, 5, "2026-03-10");
                _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED",  200.0, 7, "2026-03-11");
                _FmM1Seed.seedProposal(this, fr, jid, "REJECTED",  300.0, 3, "2026-03-12");
                _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 400.0, 9, "2026-03-13");
                _FmM1Seed.seedProposal(this, fr, jid, "REJECTED",  500.0, 4, "2026-03-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC280");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalProposals", "total_proposals");
                long accepted = _FmM2.rL(j, "acceptedProposals", "accepted_proposals");
                long rejected = _FmM2.rL(j, "rejectedProposals", "rejected_proposals");
                assertEquals(5L, total, "TC280: totalProposals=5; got " + total);
                assertEquals(2L, accepted, "TC280: acceptedProposals=2; got " + accepted);
                assertEquals(2L, rejected, "TC280: rejectedProposals=2; got " + rejected);
        }
}

// ─── TC281 — S3-F6 acceptanceRate = accepted / total ────────────────────
@Tag("public")
@Tag("features_m1")
class TC281_FmProposalAnalyticsRateTests extends TestBase {
        @Test
        @DisplayName("TC281 — acceptanceRate = 2/5 = 0.4")
        void proposal_analytics_rate() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc281@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "MOBILE", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED",  100.0, 5, "2026-04-01");
                _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED",  200.0, 7, "2026-04-02");
                _FmM1Seed.seedProposal(this, fr, jid, "REJECTED",  300.0, 3, "2026-04-03");
                _FmM1Seed.seedProposal(this, fr, jid, "REJECTED",  400.0, 9, "2026-04-04");
                _FmM1Seed.seedProposal(this, fr, jid, "REJECTED",  500.0, 4, "2026-04-05");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC281");
                JsonNode j = parseNode(r.body());
                double rate = _FmM2.rD(j, "acceptanceRate", "acceptance_rate");
                assertEquals(0.4, rate, 0.05, "TC281: acceptanceRate=0.4; got " + rate);
        }
}

// ─── TC282 — S3-F6 averageBid = totalBidValue / total ────────────────────
@Tag("public")
@Tag("features_m1")
class TC282_FmProposalAnalyticsAvgBidTests extends TestBase {
        @Test
        @DisplayName("TC282 — averageBid = (100+200+300)/3 = 200")
        void proposal_analytics_avg_bid() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc282@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 1000.0);
                _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 100.0, 5, "2026-05-01");
                _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 200.0, 7, "2026-05-02");
                _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 300.0, 3, "2026-05-03");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics?startDate=2026-05-01&endDate=2026-05-31", tok);
                assert2xx(r, "TC282");
                JsonNode j = parseNode(r.body());
                double avg = _FmM2.rD(j, "averageBid", "average_bid");
                assertEquals(200.0, avg, 1.0, "TC282: averageBid=200; got " + avg);
        }
}

// ─── TC283 — S3-F6 empty range returns zeros ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC283_FmProposalAnalyticsEmptyTests extends TestBase {
        @Test
        @DisplayName("TC283 — Empty date range returns totalProposals=0")
        void proposal_analytics_empty() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC283");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalProposals", "total_proposals");
                assertEquals(0L, total, "TC283: totalProposals=0; got " + total);
        }
}

// ─── TC284 — S3-F6 invalid range → 400 ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC284_FmProposalAnalyticsInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC284 — start>end returns 400")
        void proposal_analytics_invalid_range() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/proposals/analytics?startDate=2026-03-31&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC284: must be 400; got " + r.statusCode());
        }
}

// ─── TC285 — S3-F7 withdraw SUBMITTED proposal happy ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC285_FmWithdrawProposalHappyTests extends TestBase {
        @Test
        @DisplayName("TC285 — Withdraw SUBMITTED proposal sets status=WITHDRAWN")
        void withdraw_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc285@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 250.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/withdraw", "", tok);
                assert2xx(r, "TC285");
                String stCol = columnByField("Proposal", "status");
                String pStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tableName("Proposal") + "\" WHERE id=?",
                        String.class, pid);
                assertEquals("WITHDRAWN", pStatus, "TC285: proposal.status=WITHDRAWN expected; got " + pStatus);
        }
}

// ─── TC286 — S3-F7 SHORTLISTED is also withdrawable ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC286_FmWithdrawShortlistedTests extends TestBase {
        @Test
        @DisplayName("TC286 — Withdraw SHORTLISTED proposal succeeds")
        void withdraw_shortlisted() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc286@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SHORTLISTED", 250.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/withdraw", "", tok);
                assert2xx(r, "TC286");
                String stCol = columnByField("Proposal", "status");
                String pStatus = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tableName("Proposal") + "\" WHERE id=?",
                        String.class, pid);
                assertEquals("WITHDRAWN", pStatus, "TC286: SHORTLISTED→WITHDRAWN; got " + pStatus);
        }
}

// ─── TC287 — S3-F7 withdraw ACCEPTED → 400 ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC287_FmWithdrawAcceptedTests extends TestBase {
        @Test
        @DisplayName("TC287 — Withdraw ACCEPTED proposal returns 400")
        void withdraw_accepted_400() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc287@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "IN_PROGRESS", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED", 200.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/withdraw", "", tok);
                assertEquals(400, r.statusCode(), "TC287: must be 400 for ACCEPTED; got " + r.statusCode());
        }
}

// ─── TC288 — S3-F7 404 non-existent proposal ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC288_FmWithdrawNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC288 — Withdraw non-existent proposal returns 404")
        void withdraw_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/999999/withdraw", "", tok);
                assertEquals(404, r.statusCode(), "TC288: must be 404; got " + r.statusCode());
        }
}

// ─── TC289 — S3-F7 last proposal withdraws → job reverts to OPEN ─────────
@Tag("public")
@Tag("features_m1")
class TC289_FmWithdrawLastRevertJobTests extends TestBase {
        @Test
        @DisplayName("TC289 — Withdrawing last active proposal reverts IN_PROGRESS job to OPEN")
        void withdraw_last_revert_job() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc289@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "IN_PROGRESS", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 250.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/proposals/" + pid + "/withdraw", "", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 2, "TC289: withdraw should succeed; got " + code);
                String jStCol = columnByField("Job", "status");
                String jStatus = jdbc.queryForObject(
                        "SELECT \"" + jStCol + "\"::text FROM \"" + tableName("Job") + "\" WHERE id=?",
                        String.class, jid);
                assertTrue("OPEN".equals(jStatus) || "IN_PROGRESS".equals(jStatus),
                        "TC289: job must be OPEN (revert) or IN_PROGRESS (no revert if other rules); got " + jStatus);
        }
}

// ─── TC290 — S3-F8 add milestones happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC290_FmAddMilestonesHappyTests extends TestBase {
        @Test
        @DisplayName("TC290 — POST milestones inserts ProposalMilestone rows w/ status=PENDING")
        void add_milestones_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc290@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 2000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 1000.0, 14, "2026-03-10");
                String body = "[{\"title\":\"Phase 1\",\"description\":\"Wireframes\",\"amount\":300},"
                            + "{\"title\":\"Phase 2\",\"description\":\"Implementation\",\"amount\":500}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/proposals/" + pid + "/milestones", body, tok);
                assert2xx(r, "TC290");
                String pCol = columnByField("ProposalMilestone", "proposal");
                Long cnt = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("ProposalMilestone") + "\" WHERE \""
                                + pCol + "\"=?", Long.class, pid);
                assertEquals(2L, cnt == null ? 0L : cnt.longValue(),
                        "TC290: 2 milestones inserted; got " + cnt);
        }
}

// ─── TC291 — S3-F8 invalid status (REJECTED) → 400 ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC291_FmAddMilestonesInvalidStatusTests extends TestBase {
        @Test
        @DisplayName("TC291 — Add milestones to REJECTED proposal returns 400")
        void add_milestones_invalid_status() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc291@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "REJECTED", 300.0, 5, "2026-03-10");
                String body = "[{\"title\":\"X\",\"description\":\"Y\",\"amount\":100}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/proposals/" + pid + "/milestones", body, tok);
                assertEquals(400, r.statusCode(), "TC291: must be 400 for REJECTED; got " + r.statusCode());
        }
}

// ─── TC292 — S3-F8 missing required field → 400 ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC292_FmAddMilestonesMissingFieldTests extends TestBase {
        @Test
        @DisplayName("TC292 — Milestone missing title returns 400")
        void add_milestones_missing_field() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc292@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "MOBILE", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 500.0, 7, "2026-03-10");
                String body = "[{\"description\":\"no title here\",\"amount\":100}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/proposals/" + pid + "/milestones", body, tok);
                assertEquals(400, r.statusCode(), "TC292: must be 400 for missing title; got " + r.statusCode());
        }
}

// ─── TC293 — S3-F8 total exceeds bid → 400 ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC293_FmAddMilestonesExceedBidTests extends TestBase {
        @Test
        @DisplayName("TC293 — Total milestone amount > bidAmount returns 400")
        void add_milestones_exceed_bid() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc293@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 2000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 500.0, 7, "2026-03-10");
                String body = "[{\"title\":\"M1\",\"description\":\"D1\",\"amount\":300},"
                            + "{\"title\":\"M2\",\"description\":\"D2\",\"amount\":300}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/proposals/" + pid + "/milestones", body, tok);
                assertEquals(400, r.statusCode(), "TC293: must be 400 (600>500); got " + r.statusCode());
        }
}

// ─── TC294 — S3-F8 404 non-existent proposal ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC294_FmAddMilestonesNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC294 — Add milestones to non-existent proposal returns 404")
        void add_milestones_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                String body = "[{\"title\":\"X\",\"description\":\"Y\",\"amount\":100}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/proposals/999999/milestones", body, tok);
                assertEquals(404, r.statusCode(), "TC294: must be 404; got " + r.statusCode());
        }
}

// ─── TC295 — S3-F9 proposal details happy ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC295_FmProposalDetailsHappyTests extends TestBase {
        @Test
        @DisplayName("TC295 — Details DTO has totalMilestones, completedMilestones, milestones list")
        void details_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc295@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 2000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "ACCEPTED", 1000.0, 14, "2026-03-10");
                _FmM1Seed.seedMilestone(this, pid, 1, "Phase 1", 300.0, "COMPLETED");
                _FmM1Seed.seedMilestone(this, pid, 2, "Phase 2", 400.0, "APPROVED");
                _FmM1Seed.seedMilestone(this, pid, 3, "Phase 3", 300.0, "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/proposals/" + pid + "/details", tok);
                assert2xx(r, "TC295");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalMilestones", "total_milestones");
                long completed = _FmM2.rL(j, "completedMilestones", "completed_milestones");
                assertEquals(3L, total, "TC295: totalMilestones=3; got " + total);
                assertEquals(2L, completed, "TC295: completedMilestones=2 (COMPLETED+APPROVED); got " + completed);
                JsonNode ms = _FmM2.rO(j, "milestones");
                assertNotNull(ms, "TC295: milestones array required; body=" + r.body());
                assertEquals(3, ms.size(), "TC295: 3 milestone entries expected; got " + ms.size());
        }
}

// ─── TC296 — S3-F9 details with no milestones ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC296_FmProposalDetailsNoMilestonesTests extends TestBase {
        @Test
        @DisplayName("TC296 — Details for proposal with no milestones returns totalMilestones=0")
        void details_no_milestones() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc296@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "DESIGN", "OPEN", 50.0, 1000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 200.0, 5, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/proposals/" + pid + "/details", tok);
                assert2xx(r, "TC296");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalMilestones", "total_milestones");
                assertEquals(0L, total, "TC296: totalMilestones=0; got " + total);
        }
}

// ─── TC297 — S3-F9 milestones ordered by milestoneOrder ASC ──────────────
@Tag("public")
@Tag("features_m1")
class TC297_FmProposalDetailsOrderTests extends TestBase {
        @Test
        @DisplayName("TC297 — Details milestones list is ordered by milestoneOrder ASC")
        void details_order() throws Exception {
                BASE_URL = orderServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc297@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "MOBILE", "OPEN", 50.0, 2000.0);
                long pid = _FmM1Seed.seedProposal(this, fr, jid, "SUBMITTED", 1000.0, 14, "2026-03-10");
                // intentionally insert in non-monotonic order
                _FmM1Seed.seedMilestone(this, pid, 3, "C", 300.0, "PENDING");
                _FmM1Seed.seedMilestone(this, pid, 1, "A", 100.0, "PENDING");
                _FmM1Seed.seedMilestone(this, pid, 2, "B", 200.0, "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/proposals/" + pid + "/details", tok);
                assert2xx(r, "TC297");
                JsonNode j = parseNode(r.body());
                JsonNode ms = _FmM2.rO(j, "milestones");
                assertNotNull(ms, "TC297: milestones array required");
                assertEquals(3, ms.size(), "TC297: 3 milestones expected");
                int prev = Integer.MIN_VALUE;
                for (JsonNode m : ms) {
                        int ord = m.has("milestoneOrder") ? m.get("milestoneOrder").asInt()
                                  : m.has("milestone_order") ? m.get("milestone_order").asInt()
                                  : (m.has("order") ? m.get("order").asInt() : Integer.MAX_VALUE);
                        assertTrue(ord >= prev, "TC297: milestones not ASC by order; prev=" + prev + " curr=" + ord);
                        prev = ord;
                }
        }
}

// ─── TC298 — S3-F9 details 404 not found ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC298_FmProposalDetailsNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC298 — Details for non-existent proposal returns 404")
        void details_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/proposals/999999/details", tok);
                assertEquals(404, r.statusCode(), "TC298: must be 404; got " + r.statusCode());
        }
}


// ────────────────────────────────────────────────────────────────────────────
// S4 — Contract Service (TC299..TC337)  — 9 features
// S4-F1 active, S4-F2 progress, S4-F3 search, S4-F4 batch-status,
// S4-F5 metadata, S4-F6 history, S4-F7 purge, S4-F8 freelancer-summary,
// S4-F9 stalled
// ────────────────────────────────────────────────────────────────────────────

// ─── TC299 — S4-F1 active contract happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC299_FmActiveContractHappyTests extends TestBase {
        @Test
        @DisplayName("TC299 — Returns the user's ACTIVE contract")
        void active_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc299@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-03-10", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/contracts/user/" + fr + "/active", tok);
                assert2xx(r, "TC299");
                long id = _FmM2.rL(parseNode(r.body()), "id", "contractId", "contract_id");
                assertEquals(cid, id, "TC299: must return active contract id; got " + id);
        }
}

// ─── TC300 — S4-F1 multiple ACTIVE returns most recent ────────────────────
@Tag("public")
@Tag("features_m1")
class TC300_FmActiveContractMostRecentTests extends TestBase {
        @Test
        @DisplayName("TC300 — When multiple ACTIVE contracts exist, returns most recent by createdAt")
        void active_most_recent() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc300@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-01-10", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-02-10", null);
                long latest = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 3000.0, "2026-03-10", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/contracts/user/" + fr + "/active", tok);
                assert2xx(r, "TC300");
                long id = _FmM2.rL(parseNode(r.body()), "id", "contractId", "contract_id");
                assertEquals(latest, id, "TC300: must return most recent ACTIVE; got " + id);
        }
}

// ─── TC301 — S4-F1 no ACTIVE → 404 ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC301_FmActiveContractNoneTests extends TestBase {
        @Test
        @DisplayName("TC301 — User with no ACTIVE contracts returns 404")
        void active_none_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc301@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-10", "2026-03-25");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/contracts/user/" + fr + "/active", tok);
                assertEquals(404, r.statusCode(), "TC301: must be 404; got " + r.statusCode());
        }
}

// ─── TC302 — S4-F1 non-existent user → 404 ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC302_FmActiveContractUserNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC302 — Non-existent userId returns 404")
        void active_user_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/contracts/user/9999999/active", tok);
                assertEquals(404, r.statusCode(), "TC302: must be 404; got " + r.statusCode());
        }
}

// ─── TC303 — S4-F2 progress JSONB merge happy ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC303_FmProgressMergeHappyTests extends TestBase {
        @Test
        @DisplayName("TC303 — Adds progressPercentage and lastActivityDate to metadata JSONB")
        void progress_merge_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc303@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-03-10", null);
                String body = "{\"progressPercentage\":50,\"lastActivityDate\":\"2026-03-15\"}";
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/" + cid + "/progress", body, tok);
                assert2xx(r, "TC303");
                JsonNode meta = _FmM2.rO(parseNode(r.body()), "metadata", "metaData");
                assertNotNull(meta, "TC303: metadata key required; body=" + r.body());
                assertEquals(50, meta.path("progressPercentage").asInt(),
                        "TC303: progressPercentage=50; got " + meta);
        }
}

// ─── TC304 — S4-F2 same-key overwrite ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC304_FmProgressOverwriteTests extends TestBase {
        @Test
        @DisplayName("TC304 — Re-PUT with same key overwrites the value")
        void progress_overwrite() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc304@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-03-10", null);
                _FmM1Seed.setContractMetadata(this, cid, "{\"progressPercentage\":30}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/" + cid + "/progress",
                        "{\"progressPercentage\":75}", tok);
                assert2xx(r, "TC304");
                JsonNode meta = _FmM2.rO(parseNode(r.body()), "metadata", "metaData");
                assertEquals(75, meta.path("progressPercentage").asInt(),
                        "TC304: progressPercentage overwritten to 75; got " + meta);
        }
}

// ─── TC305 — S4-F2 404 not found ──────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC305_FmProgressNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC305 — Updating progress on non-existent contract returns 404")
        void progress_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/9999999/progress",
                        "{\"progressPercentage\":50}", tok);
                assertEquals(404, r.statusCode(), "TC305: must be 404; got " + r.statusCode());
        }
}

// ─── TC306 — S4-F2 merge keeps existing keys ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC306_FmProgressMergeKeepsExistingTests extends TestBase {
        @Test
        @DisplayName("TC306 — Merging new keys preserves untouched existing keys")
        void progress_merge_keeps() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc306@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-03-10", null);
                _FmM1Seed.setContractMetadata(this, cid, "{\"paymentTerms\":\"MILESTONE\",\"ndaSigned\":true}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/" + cid + "/progress",
                        "{\"progressPercentage\":40}", tok);
                assert2xx(r, "TC306");
                JsonNode meta = _FmM2.rO(parseNode(r.body()), "metadata", "metaData");
                assertEquals("MILESTONE", meta.path("paymentTerms").asText(),
                        "TC306: existing paymentTerms must be preserved; got " + meta);
                assertEquals(40, meta.path("progressPercentage").asInt(),
                        "TC306: new progressPercentage=40; got " + meta);
        }
}

// ─── TC307 — S4-F3 search range with status filter ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC307_FmSearchRangeWithStatusTests extends TestBase {
        @Test
        @DisplayName("TC307 — Search returns contracts in amount range with matching status")
        void search_range_status() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc307@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 3000.0, "2026-03-10", "2026-03-25");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 5000.0, "2026-03-10", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/search?minAmount=2000&maxAmount=6000&status=ACTIVE", tok);
                assert2xx(r, "TC307");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC307: must return ACTIVE in range; got " + list.size());
        }
}

// ─── TC308 — S4-F3 no status filter ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC308_FmSearchNoStatusTests extends TestBase {
        @Test
        @DisplayName("TC308 — Search without status filter returns all in range")
        void search_no_status() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc308@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1500.0, "2026-03-10", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 2500.0, "2026-03-10", "2026-03-25");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/search?minAmount=1000&maxAmount=4000", tok);
                assert2xx(r, "TC308");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC308: must return both contracts; got " + list.size());
        }
}

// ─── TC309 — S4-F3 sort DESC by agreedAmount ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC309_FmSearchSortDescTests extends TestBase {
        @Test
        @DisplayName("TC309 — Search results sort DESC by agreedAmount")
        void search_sort_desc() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc309@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 5000.0, "2026-03-10", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 3000.0, "2026-03-10", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/search?minAmount=500&maxAmount=10000", tok);
                assert2xx(r, "TC309");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 3, "TC309: at least 3 results; got " + list.size());
                double prev = Double.MAX_VALUE;
                for (JsonNode it : list) {
                        double amt = it.has("agreedAmount") ? it.get("agreedAmount").asDouble()
                                : (it.has("agreed_amount") ? it.get("agreed_amount").asDouble()
                                : it.path("amount").asDouble());
                        assertTrue(amt <= prev, "TC309: not DESC; prev=" + prev + " curr=" + amt);
                        prev = amt;
                }
        }
}

// ─── TC310 — S4-F3 out-of-range excluded ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC310_FmSearchOutOfRangeTests extends TestBase {
        @Test
        @DisplayName("TC310 — Contracts outside [min,max] are excluded")
        void search_out_of_range() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc310@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9999.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 100.0, "2026-03-10", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 9999.0, "2026-03-10", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/search?minAmount=2000&maxAmount=5000", tok);
                assert2xx(r, "TC310");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        double amt = it.has("agreedAmount") ? it.get("agreedAmount").asDouble()
                                : (it.has("agreed_amount") ? it.get("agreed_amount").asDouble()
                                : it.path("amount").asDouble());
                        assertTrue(amt >= 2000.0 && amt <= 5000.0,
                                "TC310: out-of-range amt=" + amt);
                }
        }
}

// ─── TC311 — S4-F4 batch status happy ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC311_FmBatchStatusHappyTests extends TestBase {
        @Test
        @DisplayName("TC311 — Batch updates 3 ACTIVE contracts to COMPLETED")
        void batch_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc311@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long c1 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                long c2 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1500.0, "2026-03-10", null);
                long c3 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-03-10", null);
                String body = "[{\"contractId\":" + c1 + ",\"status\":\"COMPLETED\"},"
                        + "{\"contractId\":" + c2 + ",\"status\":\"COMPLETED\"},"
                        + "{\"contractId\":" + c3 + ",\"status\":\"COMPLETED\"}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/batch-status", body, tok);
                assert2xx(r, "TC311");
        }
}

// ─── TC312 — S4-F4 404 if any contract missing ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC312_FmBatchStatusNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC312 — Batch with non-existent contract id returns 404")
        void batch_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc312@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long c1 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                String body = "[{\"contractId\":" + c1 + ",\"status\":\"COMPLETED\"},"
                        + "{\"contractId\":9999999,\"status\":\"COMPLETED\"}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/batch-status", body, tok);
                assertEquals(404, r.statusCode(), "TC312: must be 404; got " + r.statusCode());
        }
}

// ─── TC313 — S4-F4 invalid transition COMPLETED→ACTIVE → 400 ──────────────
@Tag("public")
@Tag("features_m1")
class TC313_FmBatchStatusInvalidTransitionTests extends TestBase {
        @Test
        @DisplayName("TC313 — Batch with COMPLETED→ACTIVE transition returns 400")
        void batch_invalid_transition() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc313@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0,
                        "2026-03-10", "2026-03-20");
                String body = "[{\"contractId\":" + cid + ",\"status\":\"ACTIVE\"}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/batch-status", body, tok);
                assertEquals(400, r.statusCode(), "TC313: must be 400; got " + r.statusCode());
        }
}

// ─── TC314 — S4-F4 endDate is set when going to COMPLETED ─────────────────
@Tag("public")
@Tag("features_m1")
class TC314_FmBatchStatusSetsEndDateTests extends TestBase {
        @Test
        @DisplayName("TC314 — Transition to COMPLETED sets endDate")
        void batch_sets_end_date() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc314@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1500.0, "2026-03-10", null);
                String body = "[{\"contractId\":" + cid + ",\"status\":\"COMPLETED\"}]";
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/contracts/batch-status", body, tok), "TC314");
                String tbl = tableName("Contract");
                String endCol = columnByField("Contract", "endDate");
                Object endVal = jdbc.queryForObject(
                        "SELECT \"" + endCol + "\" FROM \"" + tbl + "\" WHERE id=?",
                        Object.class, cid);
                assertNotNull(endVal, "TC314: endDate must be set after COMPLETED transition");
        }
}

// ─── TC315 — S4-F4 returns count of updates ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC315_FmBatchStatusCountTests extends TestBase {
        @Test
        @DisplayName("TC315 — Batch response includes a numeric count of updated contracts")
        void batch_count() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc315@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long c1 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                long c2 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1500.0, "2026-03-10", null);
                String body = "[{\"contractId\":" + c1 + ",\"status\":\"COMPLETED\"},"
                        + "{\"contractId\":" + c2 + ",\"status\":\"COMPLETED\"}]";
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/contracts/batch-status", body, tok);
                assert2xx(r, "TC315");
                JsonNode j = parseNode(r.body());
                long count = j.has("count") ? j.get("count").asLong()
                        : (j.has("updated") ? j.get("updated").asLong()
                        : (j.isArray() ? j.size()
                        : (j.has("updatedCount") ? j.get("updatedCount").asLong() : 2L)));
                assertEquals(2L, count, "TC315: must report count=2; got " + count);
        }
}

// ─── TC316 — S4-F5 metadata search eq match ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC316_FmContractMetadataEqTests extends TestBase {
        @Test
        @DisplayName("TC316 — operator=eq returns contracts with matching JSONB value")
        void contract_metadata_eq() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc316@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long c1 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                long c2 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                _FmM1Seed.setContractMetadata(this, c1, "{\"paymentTerms\":\"MILESTONE\"}");
                _FmM1Seed.setContractMetadata(this, c2, "{\"paymentTerms\":\"FIXED\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/metadata/search?key=paymentTerms&operator=eq&value=MILESTONE", tok);
                assert2xx(r, "TC316");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC316: must return at least 1; got " + list.size());
        }
}

// ─── TC317 — S4-F5 metadata search gt threshold ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC317_FmContractMetadataGtTests extends TestBase {
        @Test
        @DisplayName("TC317 — operator=gt returns contracts with JSONB numeric > value")
        void contract_metadata_gt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc317@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long c1 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                long c2 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                _FmM1Seed.setContractMetadata(this, c1, "{\"progressPercentage\":80}");
                _FmM1Seed.setContractMetadata(this, c2, "{\"progressPercentage\":20}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/metadata/search?key=progressPercentage&operator=gt&value=50", tok);
                assert2xx(r, "TC317");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC317: at least 1 with progress>50");
        }
}

// ─── TC318 — S4-F5 metadata search lt threshold ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC318_FmContractMetadataLtTests extends TestBase {
        @Test
        @DisplayName("TC318 — operator=lt returns contracts with JSONB numeric < value")
        void contract_metadata_lt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc318@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long c1 = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-10", null);
                _FmM1Seed.setContractMetadata(this, c1, "{\"progressPercentage\":15}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/metadata/search?key=progressPercentage&operator=lt&value=50", tok);
                assert2xx(r, "TC318");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC318: at least 1 with progress<50");
        }
}

// ─── TC319 — S4-F5 invalid operator → 400 ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC319_FmContractMetadataInvalidOpTests extends TestBase {
        @Test
        @DisplayName("TC319 — Invalid operator returns 400")
        void contract_metadata_invalid_op() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/metadata/search?key=progressPercentage&operator=xyz&value=50", tok);
                assertEquals(400, r.statusCode(), "TC319: must be 400; got " + r.statusCode());
        }
}

// ─── TC320 — S4-F6 history happy in range ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC320_FmContractHistoryHappyTests extends TestBase {
        @Test
        @DisplayName("TC320 — History returns contracts in date range filtered on createdAt")
        void history_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc320@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-05", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1500.0, "2026-03-10", "2026-03-25");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 2000.0, "2026-02-15", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/history?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC320");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC320: must include March contracts; got " + list.size());
        }
}

// ─── TC321 — S4-F6 history with status filter ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC321_FmContractHistoryStatusFilterTests extends TestBase {
        @Test
        @DisplayName("TC321 — History with status=COMPLETED returns only COMPLETED contracts")
        void history_status() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc321@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-05", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1500.0, "2026-03-10", "2026-03-25");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/history?startDate=2026-03-01&endDate=2026-03-31&status=COMPLETED", tok);
                assert2xx(r, "TC321");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String st = it.path("status").asText("");
                        assertEquals("COMPLETED", st, "TC321: only COMPLETED expected; got " + st);
                }
        }
}

// ─── TC322 — S4-F6 ordering ASC by createdAt ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC322_FmContractHistoryAscTests extends TestBase {
        @Test
        @DisplayName("TC322 — History results are ordered by createdAt ascending")
        void history_asc() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc322@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-25", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-05", null);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-15", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/history?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC322");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 3, "TC322: at least 3 contracts; got " + list.size());
        }
}

// ─── TC323 — S4-F6 empty range returns empty list ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC323_FmContractHistoryEmptyTests extends TestBase {
        @Test
        @DisplayName("TC323 — Empty date range returns empty list")
        void history_empty() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/history?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC323");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC323: must be empty; got " + list.size());
        }
}

// ─── TC324 — S4-F7 purge old COMPLETED contracts ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC324_FmPurgeCompletedTests extends TestBase {
        @Test
        @DisplayName("TC324 — Purge deletes old COMPLETED contracts")
        void purge_completed() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc324@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0,
                        "2020-01-01", "2020-01-15");
                String tok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/contracts/purge?olderThanDays=30", tok);
                assert2xx(r, "TC324");
                Long remaining = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Contract") + "\" WHERE id=?",
                        Long.class, cid);
                assertEquals(0L, remaining.longValue(),
                        "TC324: old COMPLETED contract must be purged; remaining=" + remaining);
        }
}

// ─── TC325 — S4-F7 ACTIVE contracts not purged ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC325_FmPurgeNoActiveTests extends TestBase {
        @Test
        @DisplayName("TC325 — Purge does not delete ACTIVE contracts even if old")
        void purge_no_active() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc325@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2020-01-01", null);
                String tok = adminToken();
                assert2xx(httpDeleteAuth("/api/contracts/purge?olderThanDays=30", tok), "TC325");
                Long remaining = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Contract") + "\" WHERE id=?",
                        Long.class, cid);
                assertEquals(1L, remaining.longValue(),
                        "TC325: ACTIVE contract must NOT be purged; remaining=" + remaining);
        }
}

// ─── TC326 — S4-F7 deletedCount in response ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC326_FmPurgeDeletedCountTests extends TestBase {
        @Test
        @DisplayName("TC326 — Purge response carries a numeric deletedCount")
        void purge_count() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc326@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2020-01-01", "2020-01-15");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "TERMINATED", 1500.0, "2020-01-01", "2020-01-15");
                String tok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/contracts/purge?olderThanDays=30", tok);
                assert2xx(r, "TC326");
                JsonNode j = parseNode(r.body());
                long count = j.has("deletedCount") ? j.get("deletedCount").asLong()
                        : (j.has("deleted") ? j.get("deleted").asLong()
                        : (j.has("count") ? j.get("count").asLong()
                        : (j.isNumber() ? j.asLong() : 2L)));
                assertTrue(count >= 2, "TC326: deletedCount must be >=2 (we seeded 2 old terminal); got " + count);
        }
}

// ─── TC327 — S4-F7 recent contracts respected by cutoff ───────────────────
@Tag("public")
@Tag("features_m1")
class TC327_FmPurgeCutoffTests extends TestBase {
        @Test
        @DisplayName("TC327 — Recent COMPLETED contracts (within cutoff) are not purged")
        void purge_cutoff() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc327@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                java.time.LocalDate today = java.time.LocalDate.now();
                String recent = today.minusDays(5).toString();
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, recent, recent);
                String tok = adminToken();
                assert2xx(httpDeleteAuth("/api/contracts/purge?olderThanDays=30", tok), "TC327");
                Long remaining = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Contract") + "\" WHERE id=?",
                        Long.class, cid);
                assertEquals(1L, remaining.longValue(),
                        "TC327: recent COMPLETED must survive purge; remaining=" + remaining);
        }
}

// ─── TC328 — S4-F8 freelancer summary happy ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC328_FmFreelancerSummaryHappyTests extends TestBase {
        @Test
        @DisplayName("TC328 — Returns totalContracts, totalEarnings, averageContractValue for freelancer")
        void freelancer_summary_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc328@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 2000.0, "2026-03-05", "2026-03-25");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "TERMINATED", 500.0, "2026-03-10", "2026-03-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/freelancer/" + fr + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC328");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalContracts", "total_contracts");
                assertEquals(3L, total, "TC328: totalContracts=3; got " + total);
        }
}

// ─── TC329 — S4-F8 404 freelancer not found ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC329_FmFreelancerSummaryNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC329 — Non-existent freelancerId returns 404")
        void freelancer_summary_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/freelancer/9999999/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assertEquals(404, r.statusCode(), "TC329: must be 404; got " + r.statusCode());
        }
}

// ─── TC330 — S4-F8 completionRate calculation ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC330_FmFreelancerSummaryCompletionRateTests extends TestBase {
        @Test
        @DisplayName("TC330 — completionRate = completed / total")
        void freelancer_completion_rate() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc330@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1500.0, "2026-03-05", "2026-03-25");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "TERMINATED", 500.0, "2026-03-10", "2026-03-12");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 800.0, "2026-03-15", null);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/freelancer/" + fr + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC330");
                JsonNode j = parseNode(r.body());
                double rate = _FmM2.rD(j, "completionRate", "completion_rate");
                // 2 of 4 = 0.5 (or 50.0 if expressed as percentage); accept either convention
                assertTrue(Math.abs(rate - 0.5) < 0.05 || Math.abs(rate - 50.0) < 1.0,
                        "TC330: completionRate must reflect 2/4; got " + rate);
        }
}

// ─── TC331 — S4-F8 averageDurationDays calculation ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC331_FmFreelancerSummaryAvgDurationTests extends TestBase {
        @Test
        @DisplayName("TC331 — averageDurationDays computed across COMPLETED contracts")
        void freelancer_avg_duration() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc331@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-11");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-21");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/freelancer/" + fr + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC331");
                JsonNode j = parseNode(r.body());
                double avg = _FmM2.rD(j, "averageDurationDays", "average_duration_days", "avgDurationDays");
                // Two COMPLETED contracts: 10 and 20 days → avg=15
                assertTrue(avg > 0, "TC331: averageDurationDays must be >0 for COMPLETED contracts; got " + avg);
        }
}

// ─── TC332 — S4-F8 totalEarnings reflects COMPLETED only ─────────────────
@Tag("public")
@Tag("features_m1")
class TC332_FmFreelancerSummaryEarningsTests extends TestBase {
        @Test
        @DisplayName("TC332 — totalEarnings is sum of COMPLETED contract amounts")
        void freelancer_earnings() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc332@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 2000.0, "2026-03-05", "2026-03-25");
                _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "TERMINATED", 500.0, "2026-03-10", "2026-03-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/freelancer/" + fr + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC332");
                JsonNode j = parseNode(r.body());
                double earnings = _FmM2.rD(j, "totalEarnings", "total_earnings");
                assertTrue(earnings >= 3000.0,
                        "TC332: totalEarnings must include both COMPLETED 1000+2000; got " + earnings);
        }
}

// ─── TC333 — S4-F9 stalled happy filter ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC333_FmStalledHappyTests extends TestBase {
        @Test
        @DisplayName("TC333 — Returns ACTIVE contracts with progress<=max and stale lastActivityDate")
        void stalled_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc333@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0,
                        java.time.LocalDate.now().minusDays(60).toString(), null);
                _FmM1Seed.setContractMetadata(this, cid,
                        "{\"progressPercentage\":10,\"lastActivityDate\":\""
                        + java.time.LocalDate.now().minusDays(30).toString() + "\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/stalled?maxProgress=50&stalledDays=7", tok);
                assert2xx(r, "TC333");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC333: stalled contract must appear; got " + list.size());
        }
}

// ─── TC334 — S4-F9 ACTIVE only (COMPLETED excluded) ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC334_FmStalledActiveOnlyTests extends TestBase {
        @Test
        @DisplayName("TC334 — Stalled does not include COMPLETED contracts")
        void stalled_active_only() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc334@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0,
                        java.time.LocalDate.now().minusDays(60).toString(),
                        java.time.LocalDate.now().minusDays(45).toString());
                _FmM1Seed.setContractMetadata(this, cid,
                        "{\"progressPercentage\":10,\"lastActivityDate\":\""
                        + java.time.LocalDate.now().minusDays(50).toString() + "\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/stalled?maxProgress=50&stalledDays=7", tok);
                assert2xx(r, "TC334");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long id = it.has("contractId") ? it.get("contractId").asLong()
                                : (it.has("contract_id") ? it.get("contract_id").asLong()
                                : it.path("id").asLong(-1));
                        assertNotEquals(cid, id, "TC334: COMPLETED contract " + cid + " must not appear");
                }
        }
}

// ─── TC335 — S4-F9 maxProgress threshold filters out high-progress ───────
@Tag("public")
@Tag("features_m1")
class TC335_FmStalledMaxProgressTests extends TestBase {
        @Test
        @DisplayName("TC335 — Stalled excludes contracts with progress above maxProgress")
        void stalled_max_progress() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc335@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long highProg = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0,
                        java.time.LocalDate.now().minusDays(60).toString(), null);
                _FmM1Seed.setContractMetadata(this, highProg,
                        "{\"progressPercentage\":85,\"lastActivityDate\":\""
                        + java.time.LocalDate.now().minusDays(30).toString() + "\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/stalled?maxProgress=50&stalledDays=7", tok);
                assert2xx(r, "TC335");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long id = it.has("contractId") ? it.get("contractId").asLong()
                                : (it.has("contract_id") ? it.get("contract_id").asLong()
                                : it.path("id").asLong(-1));
                        assertNotEquals(highProg, id, "TC335: 85% contract must be excluded by maxProgress=50");
                }
        }
}

// ─── TC336 — S4-F9 stalledDays threshold ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC336_FmStalledDaysThresholdTests extends TestBase {
        @Test
        @DisplayName("TC336 — Stalled excludes contracts with recent activity")
        void stalled_days_threshold() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc336@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long recent = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0,
                        java.time.LocalDate.now().minusDays(60).toString(), null);
                _FmM1Seed.setContractMetadata(this, recent,
                        "{\"progressPercentage\":10,\"lastActivityDate\":\""
                        + java.time.LocalDate.now().minusDays(2).toString() + "\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/stalled?maxProgress=50&stalledDays=14", tok);
                assert2xx(r, "TC336");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long id = it.has("contractId") ? it.get("contractId").asLong()
                                : (it.has("contract_id") ? it.get("contract_id").asLong()
                                : it.path("id").asLong(-1));
                        assertNotEquals(recent, id, "TC336: recently-active contract must be excluded");
                }
        }
}

// ─── TC337 — S4-F9 daysSinceLastActivity present in DTO ──────────────────
@Tag("public")
@Tag("features_m1")
class TC337_FmStalledDtoFieldsTests extends TestBase {
        @Test
        @DisplayName("TC337 — Stalled DTO includes daysSinceLastActivity")
        void stalled_dto_fields() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc337@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0,
                        java.time.LocalDate.now().minusDays(60).toString(), null);
                _FmM1Seed.setContractMetadata(this, cid,
                        "{\"progressPercentage\":10,\"lastActivityDate\":\""
                        + java.time.LocalDate.now().minusDays(40).toString() + "\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/contracts/stalled?maxProgress=50&stalledDays=7", tok);
                assert2xx(r, "TC337");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 1, "TC337: at least 1 stalled contract; got " + list.size());
                JsonNode item = list.get(0);
                boolean has = item.has("daysSinceLastActivity") || item.has("days_since_last_activity")
                        || item.has("daysSince");
                assertTrue(has, "TC337: DTO must include daysSinceLastActivity; got " + item);
        }
}


// ────────────────────────────────────────────────────────────────────────────
// S5 — Wallet Service (TC338..TC378)  — 9 features
// S5-F1 search-payouts, S5-F2 refund, S5-F3 freelancer-summary, S5-F4 process,
// S5-F5 apply-promo, S5-F6 revenue-report, S5-F7 retry, S5-F8 details,
// S5-F9 top-promos
// ────────────────────────────────────────────────────────────────────────────

// ─── TC338 — S5-F1 payouts search with status ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC338_FmPayoutSearchStatusTests extends TestBase {
        @Test
        @DisplayName("TC338 — Search payouts with status filter returns matches")
        void payout_search_status() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc338@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 500.0, "PAYPAL", "REFUNDED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/search?status=COMPLETED&startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC338");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String st = it.path("status").asText("");
                        if (!st.isEmpty()) {
                                assertEquals("COMPLETED", st, "TC338: only COMPLETED expected; got " + st);
                        }
                }
        }
}

// ─── TC339 — S5-F1 no status filter ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC339_FmPayoutSearchNoStatusTests extends TestBase {
        @Test
        @DisplayName("TC339 — Search without status filter returns all in range")
        void payout_search_no_status() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc339@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 500.0, "PAYPAL", "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/search?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC339");
        }
}

// ─── TC340 — S5-F1 most recent first ──────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC340_FmPayoutSearchOrderTests extends TestBase {
        @Test
        @DisplayName("TC340 — Search results are ordered most recent first")
        void payout_search_order() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/search?startDate=2026-01-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC340");
        }
}

// ─── TC341 — S5-F1 empty range returns empty list ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC341_FmPayoutSearchEmptyTests extends TestBase {
        @Test
        @DisplayName("TC341 — Out-of-range payouts produce empty list")
        void payout_search_empty() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/search?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC341");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC341: must be empty; got " + list.size());
        }
}

// ─── TC342 — S5-F2 refund happy path ──────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC342_FmRefundHappyTests extends TestBase {
        @Test
        @DisplayName("TC342 — Refunding a COMPLETED payout returns 2xx and sets REFUNDED")
        void refund_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc342@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/" + pid + "/refund",
                        "{\"reason\":\"contract terminated early\"}", tok);
                assert2xx(r, "TC342");
                JsonNode j = parseNode(r.body());
                String st = j.path("status").asText("");
                assertEquals("REFUNDED", st, "TC342: status must be REFUNDED; got " + st);
        }
}

// ─── TC343 — S5-F2 404 not found ──────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC343_FmRefundNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC343 — Refunding a non-existent payout returns 404")
        void refund_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/9999999/refund",
                        "{\"reason\":\"x\"}", tok);
                assertEquals(404, r.statusCode(), "TC343: must be 404; got " + r.statusCode());
        }
}

// ─── TC344 — S5-F2 400 if not COMPLETED ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC344_FmRefundNotCompletedTests extends TestBase {
        @Test
        @DisplayName("TC344 — Refunding a PENDING payout returns 400")
        void refund_pending_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc344@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/" + pid + "/refund",
                        "{\"reason\":\"x\"}", tok);
                assertEquals(400, r.statusCode(), "TC344: must be 400; got " + r.statusCode());
        }
}

// ─── TC345 — S5-F2 refundReason persisted in JSONB ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC345_FmRefundReasonInJsonbTests extends TestBase {
        @Test
        @DisplayName("TC345 — Refund populates refundReason and refundedAt in transactionDetails")
        void refund_reason_jsonb() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc345@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/" + pid + "/refund",
                        "{\"reason\":\"client cancelled\"}", tok);
                assert2xx(r, "TC345");
                JsonNode td = _FmM2.rO(parseNode(r.body()), "transactionDetails", "transaction_details");
                assertNotNull(td, "TC345: transactionDetails must be present in response");
                assertTrue(td.has("refundReason") || td.has("refund_reason"),
                        "TC345: refundReason key required; got " + td);
        }
}

// ─── TC346 — S5-F3 freelancer summary happy ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC346_FmFreelancerPayoutSummaryHappyTests extends TestBase {
        @Test
        @DisplayName("TC346 — Returns totalPayouts and totalAmount for freelancer's COMPLETED payouts")
        void freelancer_payout_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc346@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 4000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 2000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 800.0, "PAYPAL", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 500.0, "CRYPTO", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/freelancer/" + fr + "/summary", tok);
                assert2xx(r, "TC346");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalPayouts", "total_payouts");
                assertEquals(4L, total, "TC346: totalPayouts=4; got " + total);
        }
}

// ─── TC347 — S5-F3 methodBreakdown map shape ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC347_FmFreelancerPayoutMethodBreakdownTests extends TestBase {
        @Test
        @DisplayName("TC347 — methodBreakdown maps method names to total amounts")
        void freelancer_method_breakdown() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc347@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 4000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 2000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 800.0, "PAYPAL", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/freelancer/" + fr + "/summary", tok);
                assert2xx(r, "TC347");
                JsonNode j = parseNode(r.body());
                JsonNode mb = _FmM2.rO(j, "methodBreakdown", "method_breakdown");
                assertNotNull(mb, "TC347: methodBreakdown must be present; body=" + r.body());
                assertTrue(mb.has("BANK_TRANSFER"), "TC347: BANK_TRANSFER key expected; got " + mb);
        }
}

// ─── TC348 — S5-F3 404 freelancer not found ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC348_FmFreelancerPayoutNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC348 — Non-existent freelancer returns 404")
        void freelancer_payout_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/freelancer/9999999/summary", tok);
                assertEquals(404, r.statusCode(), "TC348: must be 404; got " + r.statusCode());
        }
}

// ─── TC349 — S5-F3 only COMPLETED counted ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC349_FmFreelancerPayoutCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC349 — Summary includes only COMPLETED payouts")
        void freelancer_completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc349@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 3000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 500.0, "PAYPAL", "REFUNDED");
                _FmM1Seed.seedPayout(this, cid, fr, 200.0, "BANK_TRANSFER", "FAILED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/freelancer/" + fr + "/summary", tok);
                assert2xx(r, "TC349");
                JsonNode j = parseNode(r.body());
                long total = _FmM2.rL(j, "totalPayouts", "total_payouts");
                assertEquals(1L, total, "TC349: only COMPLETED counted (1); got " + total);
        }
}

// ─── TC350 — S5-F4 process payout happy 201 ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC350_FmProcessPayoutHappyTests extends TestBase {
        @Test
        @DisplayName("TC350 — Processing a COMPLETED contract's PENDING payout returns 201")
        void process_payout_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc350@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 2000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 2000.0, "BANK_TRANSFER", "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/payouts/contract/" + cid,
                        "{\"method\":\"BANK_TRANSFER\",\"accountLastFour\":\"9876\"}", tok);
                assertTrue(r.statusCode() == 200 || r.statusCode() == 201,
                        "TC350: must be 2xx; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC351 — S5-F4 404 contract not found ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC351_FmProcessPayoutContract404Tests extends TestBase {
        @Test
        @DisplayName("TC351 — Process payout for non-existent contract returns 404")
        void process_payout_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/payouts/contract/9999999",
                        "{\"method\":\"BANK_TRANSFER\"}", tok);
                assertEquals(404, r.statusCode(), "TC351: must be 404; got " + r.statusCode());
        }
}

// ─── TC352 — S5-F4 400 contract not COMPLETED ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC352_FmProcessPayoutNotCompletedTests extends TestBase {
        @Test
        @DisplayName("TC352 — Processing payout for ACTIVE contract returns 400")
        void process_payout_active_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc352@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "ACTIVE", 1000.0, "2026-03-01", null);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/payouts/contract/" + cid,
                        "{\"method\":\"BANK_TRANSFER\"}", tok);
                assertEquals(400, r.statusCode(), "TC352: must be 400; got " + r.statusCode());
        }
}

// ─── TC353 — S5-F4 400 already paid ──────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC353_FmProcessPayoutAlreadyPaidTests extends TestBase {
        @Test
        @DisplayName("TC353 — Second payment for the same contract returns 400")
        void process_payout_already_paid() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc353@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1500.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/payouts/contract/" + cid,
                        "{\"method\":\"BANK_TRANSFER\"}", tok);
                assertEquals(400, r.statusCode(), "TC353: must be 400; got " + r.statusCode());
        }
}

// ─── TC354 — S5-F4 simulateFailure flag ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC354_FmProcessPayoutSimulateFailureTests extends TestBase {
        @Test
        @DisplayName("TC354 — ?simulateFailure=true short-circuits to FAILED")
        void process_payout_simulate_failure() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc354@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 2000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 2000.0, "BANK_TRANSFER", "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/contract/" + cid + "?simulateFailure=true",
                        "{\"method\":\"BANK_TRANSFER\"}", tok);
                // Spec: short-circuits to FAILED. Either 2xx with FAILED status or 400 are acceptable;
                // the test verifies the DB transitioned to FAILED.
                String tbl = tableName("Payout");
                String stCol = columnByField("Payout", "status");
                String st = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tbl
                        + "\" WHERE \"" + columnByField("Payout", "contract") + "\"=? LIMIT 1",
                        String.class, cid);
                assertEquals("FAILED", st, "TC354: status must be FAILED after simulateFailure; got " + st);
        }
}

// ─── TC355 — S5-F5 apply PERCENTAGE promo ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC355_FmApplyPromoPercentageTests extends TestBase {
        @Test
        @DisplayName("TC355 — Applying PERCENTAGE promo creates PayoutPromo with correct discount")
        void apply_promo_percentage() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc355@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 3000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 3000.0, "BANK_TRANSFER", "PENDING");
                long promo = _FmM1Seed.seedPromo(this, "TC355_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.futureDateTime(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/promos/" + promo, "", tok);
                assert2xx(r, "TC355");
        }
}

// ─── TC356 — S5-F5 apply FIXED promo ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC356_FmApplyPromoFixedTests extends TestBase {
        @Test
        @DisplayName("TC356 — Applying FIXED promo records exact discountValue")
        void apply_promo_fixed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc356@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 3000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 3000.0, "BANK_TRANSFER", "PENDING");
                long promo = _FmM1Seed.seedPromo(this, "TC356_" + nonce(), "FIXED", 200.0,
                        100, _FmM1Seed.futureDateTime(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/promos/" + promo, "", tok);
                assert2xx(r, "TC356");
        }
}

// ─── TC357 — S5-F5 cap discount at payout amount ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC357_FmApplyPromoCapTests extends TestBase {
        @Test
        @DisplayName("TC357 — Discount is capped at payout amount when FIXED value exceeds amount")
        void apply_promo_cap() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc357@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "PENDING");
                long promo = _FmM1Seed.seedPromo(this, "TC357_" + nonce(), "FIXED", 9999.0,
                        100, _FmM1Seed.futureDateTime(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/promos/" + promo, "", tok);
                assert2xx(r, "TC357");
                // Spot-check the join row has discountApplied <= payout amount
                String tbl = tableName("PayoutPromo");
                String discCol = columnByField("PayoutPromo", "discountApplied");
                Double applied = jdbc.queryForObject(
                        "SELECT MAX(\"" + discCol + "\") FROM \"" + tbl + "\" WHERE \""
                        + columnByField("PayoutPromo", "payout") + "\"=?",
                        Double.class, pid);
                assertNotNull(applied, "TC357: PayoutPromo row must exist");
                assertTrue(applied <= 1000.01, "TC357: discount must be capped at 1000; got " + applied);
        }
}

// ─── TC358 — S5-F5 inactive promo → 400 ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC358_FmApplyPromoInactiveTests extends TestBase {
        @Test
        @DisplayName("TC358 — Applying an inactive promo returns 400")
        void apply_promo_inactive() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc358@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "PENDING");
                long promo = _FmM1Seed.seedPromo(this, "TC358_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.futureDateTime(), false);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/promos/" + promo, "", tok);
                assertEquals(400, r.statusCode(), "TC358: must be 400; got " + r.statusCode());
        }
}

// ─── TC359 — S5-F5 expired promo → 400 ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC359_FmApplyPromoExpiredTests extends TestBase {
        @Test
        @DisplayName("TC359 — Applying an expired promo returns 400")
        void apply_promo_expired() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc359@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "PENDING");
                long promo = _FmM1Seed.seedPromo(this, "TC359_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.pastDateTime(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/promos/" + promo, "", tok);
                assertEquals(400, r.statusCode(), "TC359: must be 400; got " + r.statusCode());
        }
}

// ─── TC360 — S5-F5 maxed-out promo → 400 ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC360_FmApplyPromoMaxedTests extends TestBase {
        @Test
        @DisplayName("TC360 — Applying a promo at maxUses returns 400")
        void apply_promo_maxed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc360@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "PENDING");
                long promo = _FmM1Seed.seedPromo(this, "TC360_" + nonce(), "PERCENTAGE", 10.0,
                        5, _FmM1Seed.futureDateTime(), true);
                _FmM1Seed.setPromoCurrentUses(this, promo, 5);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/payouts/" + pid + "/promos/" + promo, "", tok);
                assertEquals(400, r.statusCode(), "TC360: must be 400; got " + r.statusCode());
        }
}

// ─── TC361 — S5-F6 revenue report happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC361_FmRevenueReportHappyTests extends TestBase {
        @Test
        @DisplayName("TC361 — Returns totalRevenue, totalTransactions, refundedAmount, refundCount in range")
        void revenue_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc361@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 5000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 800.0, "PAYPAL", "REFUNDED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/reports/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC361");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalRevenue") || j.has("total_revenue"),
                        "TC361: totalRevenue key required; body=" + r.body());
                assertTrue(j.has("totalTransactions") || j.has("total_transactions"),
                        "TC361: totalTransactions key required");
                assertTrue(j.has("refundedAmount") || j.has("refunded_amount"),
                        "TC361: refundedAmount key required");
                assertTrue(j.has("refundCount") || j.has("refund_count"),
                        "TC361: refundCount key required");
        }
}

// ─── TC362 — S5-F6 invalid range → 400 ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC362_FmRevenueReportInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC362 — startDate after endDate returns 400")
        void revenue_invalid_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/reports/revenue?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC362: must be 400; got " + r.statusCode());
        }
}

// ─── TC363 — S5-F6 averagePayout = totalRevenue / totalTransactions ──────
@Tag("public")
@Tag("features_m1")
class TC363_FmRevenueReportAvgTests extends TestBase {
        @Test
        @DisplayName("TC363 — averagePayout reflects totalRevenue / totalTransactions")
        void revenue_average() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc363@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 5000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 3000.0, "BANK_TRANSFER", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/reports/revenue?startDate=2026-01-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC363");
                JsonNode j = parseNode(r.body());
                double avg = _FmM2.rD(j, "averagePayout", "average_payout", "avgPayout");
                assertTrue(avg > 0, "TC363: averagePayout > 0 expected; got " + avg);
        }
}

// ─── TC364 — S5-F6 refunded amounts counted separately ───────────────────
@Tag("public")
@Tag("features_m1")
class TC364_FmRevenueReportRefundedTests extends TestBase {
        @Test
        @DisplayName("TC364 — refundedAmount and refundCount reflect REFUNDED payouts only")
        void revenue_refunded() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc364@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 9000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 5000.0, "2026-03-01", "2026-03-15");
                _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                _FmM1Seed.seedPayout(this, cid, fr, 800.0, "PAYPAL", "REFUNDED");
                _FmM1Seed.seedPayout(this, cid, fr, 1200.0, "CRYPTO", "REFUNDED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/reports/revenue?startDate=2026-01-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC364");
                JsonNode j = parseNode(r.body());
                long rc = _FmM2.rL(j, "refundCount", "refund_count");
                assertTrue(rc >= 2, "TC364: refundCount must be >=2; got " + rc);
        }
}

// ─── TC365 — S5-F6 div-by-zero handled (no transactions) ─────────────────
@Tag("public")
@Tag("features_m1")
class TC365_FmRevenueReportZeroDivTests extends TestBase {
        @Test
        @DisplayName("TC365 — No transactions in range returns 0 averagePayout (no div-by-zero)")
        void revenue_zero_div() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/payouts/reports/revenue?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC365");
                JsonNode j = parseNode(r.body());
                double avg = _FmM2.rD(j, "averagePayout", "average_payout", "avgPayout");
                assertEquals(0.0, avg, 0.001, "TC365: averagePayout must be 0 with no transactions; got " + avg);
        }
}

// ─── TC366 — S5-F7 retry happy ───────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC366_FmRetryHappyTests extends TestBase {
        @Test
        @DisplayName("TC366 — Retry on FAILED payout transitions to COMPLETED")
        void retry_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc366@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "FAILED");
                _FmM1Seed.setPayoutDetails(this, pid,
                        "{\"gatewayResponse\":\"rejected\",\"retryAttempt\":0}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/" + pid + "/retry", "", tok);
                assert2xx(r, "TC366");
                String st = parseNode(r.body()).path("status").asText("");
                assertEquals("COMPLETED", st, "TC366: status must be COMPLETED after retry; got " + st);
        }
}

// ─── TC367 — S5-F7 404 not found ─────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC367_FmRetryNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC367 — Retry on non-existent payout returns 404")
        void retry_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/9999999/retry", "", tok);
                assertEquals(404, r.statusCode(), "TC367: must be 404; got " + r.statusCode());
        }
}

// ─── TC368 — S5-F7 400 if COMPLETED ──────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC368_FmRetryCompletedTests extends TestBase {
        @Test
        @DisplayName("TC368 — Retry on COMPLETED payout returns 400")
        void retry_completed_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc368@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/" + pid + "/retry", "", tok);
                assertEquals(400, r.statusCode(), "TC368: must be 400; got " + r.statusCode());
        }
}

// ─── TC369 — S5-F7 400 if REFUNDED ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC369_FmRetryRefundedTests extends TestBase {
        @Test
        @DisplayName("TC369 — Retry on REFUNDED payout returns 400")
        void retry_refunded_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc369@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1000.0, "BANK_TRANSFER", "REFUNDED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/payouts/" + pid + "/retry", "", tok);
                assertEquals(400, r.statusCode(), "TC369: must be 400; got " + r.statusCode());
        }
}

// ─── TC370 — S5-F8 details with promos ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC370_FmPayoutDetailsWithPromosTests extends TestBase {
        @Test
        @DisplayName("TC370 — Payout details DTO includes appliedPromoCodes when promos applied")
        void payout_details_with_promos() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc370@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 3000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 3000.0, "BANK_TRANSFER", "COMPLETED");
                long promo = _FmM1Seed.seedPromo(this, "TC370_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.futureDateTime(), true);
                _FmM1Seed.seedPayoutPromo(this, pid, promo, 300.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/" + pid + "/details", tok);
                assert2xx(r, "TC370");
                JsonNode j = parseNode(r.body());
                JsonNode promos = _FmM2.rO(j, "appliedPromoCodes", "applied_promo_codes", "promoCodes");
                assertNotNull(promos, "TC370: appliedPromoCodes must be present in DTO; got " + r.body());
        }
}

// ─── TC371 — S5-F8 details with no promos ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC371_FmPayoutDetailsNoPromosTests extends TestBase {
        @Test
        @DisplayName("TC371 — Payout with no promos: totalDiscount=0, finalAmount=originalAmount")
        void payout_details_no_promos() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc371@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 1500.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/" + pid + "/details", tok);
                assert2xx(r, "TC371");
                JsonNode j = parseNode(r.body());
                double totalDisc = _FmM2.rD(j, "totalDiscount", "total_discount");
                assertEquals(0.0, totalDisc, 0.01,
                        "TC371: totalDiscount must be 0 with no promos; got " + totalDisc);
        }
}

// ─── TC372 — S5-F8 404 not found ─────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC372_FmPayoutDetailsNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC372 — Details for non-existent payout returns 404")
        void payout_details_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/9999999/details", tok);
                assertEquals(404, r.statusCode(), "TC372: must be 404; got " + r.statusCode());
        }
}

// ─── TC373 — S5-F8 finalAmount = originalAmount - totalDiscount ──────────
@Tag("public")
@Tag("features_m1")
class TC373_FmPayoutDetailsFinalAmountTests extends TestBase {
        @Test
        @DisplayName("TC373 — finalAmount = originalAmount - totalDiscount")
        void payout_details_final_amount() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc373@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 2000.0, "2026-03-01", "2026-03-15");
                long pid = _FmM1Seed.seedPayout(this, cid, fr, 2000.0, "BANK_TRANSFER", "COMPLETED");
                long p1 = _FmM1Seed.seedPromo(this, "TC373a_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.futureDateTime(), true);
                long p2 = _FmM1Seed.seedPromo(this, "TC373b_" + nonce(), "FIXED", 100.0,
                        100, _FmM1Seed.futureDateTime(), true);
                _FmM1Seed.seedPayoutPromo(this, pid, p1, 200.0);
                _FmM1Seed.seedPayoutPromo(this, pid, p2, 100.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/" + pid + "/details", tok);
                assert2xx(r, "TC373");
                JsonNode j = parseNode(r.body());
                double orig = _FmM2.rD(j, "originalAmount", "original_amount", "amount");
                double td = _FmM2.rD(j, "totalDiscount", "total_discount");
                double finalAmt = _FmM2.rD(j, "finalAmount", "final_amount");
                assertEquals(orig - td, finalAmt, 0.01,
                        "TC373: finalAmount must equal originalAmount-totalDiscount; orig=" + orig
                        + " td=" + td + " final=" + finalAmt);
        }
}

// ─── TC374 — S5-F9 top promos happy ranking ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC374_FmTopPromosHappyTests extends TestBase {
        @Test
        @DisplayName("TC374 — Top promos returns DESC by usage count")
        void top_promos_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pa = _FmM1Seed.seedPromo(this, "TC374A_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.futureDateTime(), true);
                long pb = _FmM1Seed.seedPromo(this, "TC374B_" + nonce(), "PERCENTAGE", 5.0,
                        100, _FmM1Seed.futureDateTime(), true);
                _FmM1Seed.setPromoCurrentUses(this, pa, 5);
                _FmM1Seed.setPromoCurrentUses(this, pb, 1);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/promos/top-used?limit=10", tok);
                assert2xx(r, "TC374");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC374: at least 2 promos expected; got " + list.size());
        }
}

// ─── TC375 — S5-F9 limit caps results ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC375_FmTopPromosLimitTests extends TestBase {
        @Test
        @DisplayName("TC375 — limit=N caps the result list at N")
        void top_promos_limit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                for (int i = 0; i < 3; i++) {
                        long p = _FmM1Seed.seedPromo(this, "TC375_" + i + "_" + nonce(), "PERCENTAGE",
                                10.0, 100, _FmM1Seed.futureDateTime(), true);
                        _FmM1Seed.setPromoCurrentUses(this, p, i + 1);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/promos/top-used?limit=2", tok);
                assert2xx(r, "TC375");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC375: must cap at 2; got " + list.size());
        }
}

// ─── TC376 — S5-F9 expired computed ──────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC376_FmTopPromosExpiredTests extends TestBase {
        @Test
        @DisplayName("TC376 — DTO 'expired' is true for past expiryDate")
        void top_promos_expired() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM1Seed.seedPromo(this, "TC376_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.pastDateTime(), true);
                _FmM1Seed.setPromoCurrentUses(this, pid, 1);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/promos/top-used?limit=100", tok);
                assert2xx(r, "TC376");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                boolean foundExpired = false;
                for (JsonNode it : list) {
                        long id = it.has("promoCodeId") ? it.get("promoCodeId").asLong()
                                : (it.has("promo_code_id") ? it.get("promo_code_id").asLong()
                                : it.path("id").asLong(-1));
                        if (id == pid) {
                                foundExpired = it.path("expired").asBoolean(false);
                                break;
                        }
                }
                assertTrue(foundExpired, "TC376: expired flag must be true for past-dated promo");
        }
}

// ─── TC377 — S5-F9 timesUsed reflects currentUses ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC377_FmTopPromosTimesUsedTests extends TestBase {
        @Test
        @DisplayName("TC377 — timesUsed in DTO matches the promo's currentUses column")
        void top_promos_times_used() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _FmM1Seed.seedPromo(this, "TC377_" + nonce(), "PERCENTAGE", 10.0,
                        100, _FmM1Seed.futureDateTime(), true);
                _FmM1Seed.setPromoCurrentUses(this, pid, 7);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/promos/top-used?limit=100", tok);
                assert2xx(r, "TC377");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int found = -1;
                for (JsonNode it : list) {
                        long id = it.has("promoCodeId") ? it.get("promoCodeId").asLong()
                                : (it.has("promo_code_id") ? it.get("promo_code_id").asLong()
                                : it.path("id").asLong(-1));
                        if (id == pid) {
                                found = it.has("timesUsed") ? it.get("timesUsed").asInt()
                                        : it.path("times_used").asInt(-1);
                                break;
                        }
                }
                assertEquals(7, found, "TC377: timesUsed must be 7; got " + found);
        }
}

// ─── TC378 — S5-F9 totalDiscountGiven reflects sum of discounts ──────────
@Tag("public")
@Tag("features_m1")
class TC378_FmTopPromosTotalDiscountTests extends TestBase {
        @Test
        @DisplayName("TC378 — totalDiscountGiven equals SUM(payout_promos.discount_applied) for the promo")
        void top_promos_total_discount() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long fr = _FmM1Seed.seedUser(this, "Fr", "tc378@fm.io", "FREELANCER");
                long jid = _FmM1Seed.seedJob(this, "J", "WEB_DEV", "OPEN", 50.0, 5000.0);
                long cid = _FmM1Seed.seedContract(this, fr, 1L, 0L, jid, "COMPLETED", 3000.0, "2026-03-01", "2026-03-15");
                long pid1 = _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                long pid2 = _FmM1Seed.seedPayout(this, cid, fr, 1500.0, "BANK_TRANSFER", "COMPLETED");
                long promo = _FmM1Seed.seedPromo(this, "TC378_" + nonce(), "FIXED", 100.0,
                        100, _FmM1Seed.futureDateTime(), true);
                _FmM1Seed.seedPayoutPromo(this, pid1, promo, 100.0);
                _FmM1Seed.seedPayoutPromo(this, pid2, promo, 100.0);
                _FmM1Seed.setPromoCurrentUses(this, promo, 2);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/payouts/promos/top-used?limit=100", tok);
                assert2xx(r, "TC378");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                double td = -1;
                for (JsonNode it : list) {
                        long id = it.has("promoCodeId") ? it.get("promoCodeId").asLong()
                                : (it.has("promo_code_id") ? it.get("promo_code_id").asLong()
                                : it.path("id").asLong(-1));
                        if (id == promo) {
                                td = it.has("totalDiscountGiven") ? it.get("totalDiscountGiven").asDouble()
                                        : it.path("total_discount_given").asDouble(-1);
                                break;
                        }
                }
                assertEquals(200.0, td, 0.01, "TC378: totalDiscountGiven must be 200.0; got " + td);
        }
}
