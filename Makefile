# Bad Usernames API — common operations
#
# Run `make help` (or just `make`) to list targets.

# Use the sample dataset for local runs unless overridden on the command line.
DATASET ?= dev/sample-bad-usernames.json
SBT     ?= sbt

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help
	@grep -E '^[a-zA-Z0-9_-]+:.*?## ' $(MAKEFILE_LIST) \
		| sort \
		| awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

.PHONY: run
run: ## Run the API locally against the sample dataset
	BAD_USERNAMES_DATASET_PATH=$(DATASET) $(SBT) run

.PHONY: test
test: ## Run the test suite
	$(SBT) test

.PHONY: compile
compile: ## Compile main + test sources
	$(SBT) Test/compile

.PHONY: fmt
fmt: ## Format all sources with scalafmt
	$(SBT) scalafmtAll scalafmtSbt

.PHONY: fmt-check
fmt-check: ## Check formatting without modifying files
	$(SBT) scalafmtCheckAll scalafmtSbtCheck

.PHONY: check
check: fmt-check test ## Format check + tests (pre-commit gate)

.PHONY: console
console: ## Start an sbt shell
	$(SBT) shell

.PHONY: clean
clean: ## Remove build artifacts
	$(SBT) clean

.PHONY: smoke
smoke: ## Curl the running API (single check + meta) on localhost:8080
	@curl -sS http://localhost:8080/api/v1/check/admin; echo
	@curl -sS http://localhost:8080/api/v1/meta; echo
