# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. I would standardize data access behind explicit repository/port contracts (as in Warehouse) and avoid mixing direct Panache active-record style calls in resources with domain logic.

Why:
1) Consistency: today Store/Product use direct entity calls while Warehouse uses a separate repository + use cases. The mixed style increases cognitive load and makes code reviews/testing uneven.
2) Testability: explicit ports let us test business rules with in-memory fakes quickly (as done for warehouse use cases) without booting Quarkus.
3) Separation of concerns: resources should orchestrate HTTP concerns; domain/use-case services should own business decisions; repositories should own persistence details.
4) Evolution: if schema or storage changes, the impact is localized.

I would not do a big-bang rewrite. I would incrementally refactor Store/Product toward the same boundary style while keeping Panache under repositories.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first (Warehouse) pros:
- Contract clarity and collaboration: backend/frontend/QA align early on request/response shapes.
- Safer changes: contract drift is visible; generated interfaces reduce handler/signature mismatch.
- Better API governance: versioning and documentation are easier to keep consistent.

OpenAPI-first cons:
- Initial overhead and generator/tooling complexity.
- Generated code can feel rigid/noisy if the team is unfamiliar with the workflow.

Code-first (Product/Store) pros:
- Faster for small/simple endpoints.
- Lower upfront tooling/process cost.

Code-first cons:
- Docs/spec may lag implementation.
- Increased risk of inconsistent error models and API behavior.

My choice: OpenAPI-first for externally consumed or business-critical APIs (like Warehouse), with lightweight code-first acceptable for internal/simple endpoints early on. Over time, I would converge all public endpoints to a shared contract-first approach.

```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would prioritize by business risk and feedback speed:

1) Fast unit tests (highest ROI):
- Focus on use-case validations and edge cases (create/replace/archive warehouse rules, location checks, capacity/stock constraints).
- These run in milliseconds and catch most regressions early.

2) Targeted API integration tests:
- Cover critical happy paths + a few key failure responses per endpoint (400/404 + state transitions).
- Keep the suite small and deterministic.

3) End-to-end smoke checks:
- Minimal set in CI to verify app boot + core endpoints in a real runtime profile.

To keep coverage effective over time:
- Treat bugs as test opportunities: every bug fix adds a regression test.
- Track critical business rules in a checklist and map each to at least one test.
- Keep tests isolated, data-seeded, and stable to avoid flaky CI.
- Run unit tests on every commit; run heavier integration suites on PR/merge.

```