# See https://tech.davis-hansson.com/p/make/
SHELL := bash
.DELETE_ON_ERROR:
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := all
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-print-directory
BIN := .tmp/bin
CACHE := .tmp/cache
LICENSE_HEADER_YEAR_RANGE := 2022-2023
CROSSTEST_VERSION := 162d496c009e2ffb1a638b4a2ea789e9cc3331bb
LICENSE_HEADER_VERSION := v1.26.1
PROTOC_VERSION ?= 24.1
GRADLE_ARGS ?=

UNAME_OS := $(shell uname -s)
UNAME_ARCH := $(shell uname -m)

.PHONY: all
all: build

$(BIN)/license-headers: Makefile
	mkdir -p $(@D)
	GOBIN=$(abspath $(BIN)) go install github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@$(LICENSE_HEADER_VERSION)

.PHONY: build
build: generate ## Build the entire project.
	./gradlew $(GRADLE_ARGS) build -x test

.PHONY: buildplugin
buildplugin: ## Build the connect-kotlin protoc plugin.
	./gradlew $(GRADLE_ARGS) protoc-gen-connect-kotlin:installDist

.PHONY: clean
clean: ## Cleans the underlying build.
	./gradlew $(GRADLE_ARGS) clean
	rm -rf examples/generated-google-java/src/main
	rm -rf examples/generated-google-javalite/src/main

	rm -rf crosstests/google-java/src/main/java/generated
	rm -rf crosstests/google-java/src/main/kotlin/generated
	rm -rf crosstests/google-javalite/src/main/java/generated
	rm -rf crosstests/google-javalite/src/main/kotlin/generated

	rm -rf protoc-gen-connect-kotlin/src/test/java/

.PHONY: crosstestserverrun
crosstestserverrun: crosstestserverstop ## Run the server for cross tests.
	docker run --rm --name serverconnect -p 8080:8080 -p 8081:8081 -d \
		bufbuild/connect-crosstest:$(CROSSTEST_VERSION) \
		/usr/local/bin/serverconnect --h1port "8080" --h2port "8081" --cert "cert/localhost.crt" --key "cert/localhost.key"
	docker run --rm --name servergrpc -p 8083:8083 -d \
		bufbuild/connect-crosstest:$(CROSSTEST_VERSION) \
		/usr/local/bin/servergrpc --port "8083" --cert "cert/localhost.crt" --key "cert/localhost.key"

.PHONY: crosstestserverstop
crosstestserverstop: ## Stop the server for cross tests.
	-docker container stop serverconnect servergrpc

.PHONY: crosstestsrun
crosstestsrun: crosstestsrunjava ## Run the cross tests.

.PHONY: crosstestsrunjava
crosstestsrunjava: ## Run the cross tests for protoc-gen-java integration.
	./gradlew $(GRADLE_ARGS) crosstest:google-java:test

ifeq ($(UNAME_OS),Darwin)
PROTOC_OS := osx
ifeq ($(UNAME_ARCH),arm64)
PROTOC_ARCH := aarch_64
else
PROTOC_ARCH := x86_64
endif
endif
ifeq ($(UNAME_OS),Linux)
PROTOC_OS = linux
PROTOC_ARCH := $(UNAME_ARCH)
endif

PROTOC := $(CACHE)/protoc-$(PROTOC_VERSION).zip
$(PROTOC):
	@if ! command -v curl >/dev/null 2>/dev/null; then echo "error: curl must be installed" >&2; exit 1; fi
	@if ! command -v unzip >/dev/null 2>/dev/null; then echo "error: unzip must be installed" >&2; exit 1; fi
	@rm -f $(BIN)/protoc
	$(eval PROTOC_TMP := $(shell mktemp -d))
	curl -sSL https://github.com/protocolbuffers/protobuf/releases/download/v$(PROTOC_VERSION)/protoc-$(PROTOC_VERSION)-$(PROTOC_OS)-$(PROTOC_ARCH).zip -o $(PROTOC_TMP)/protoc.zip
	@mkdir -p $(BIN)
	unzip -q $(PROTOC_TMP)/protoc.zip -d $(dir $(BIN)) bin/protoc
	@rm -rf $(PROTOC_TMP)
	@mkdir -p $(dir $@)
	@touch $@

.PHONY: generate
generate: $(PROTOC) buildplugin generatecrosstests generateexamples ## Generate proto files for the entire project.
	buf generate --template protoc-gen-connect-kotlin/buf.gen.yaml -o protoc-gen-connect-kotlin
	buf generate --template extensions/buf.gen.yaml -o extensions buf.build/googleapis/googleapis
	make licenseheaders

.PHONY: generatecrosstests
generatecrosstests: $(PROTOC) buildplugin ## Generate protofiles for cross tests.
	buf generate --template crosstests/buf.gen.yaml -o crosstests

.PHONY: generateexamples
generateexamples: $(PROTOC) buildplugin ## Generate proto files for example apps.
	buf generate --template examples/buf.gen.yaml -o examples buf.build/connectrpc/eliza

.PHONY: help
help: ## Describe useful make targets.
	grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-30s %s\n", $$1, $$2}'

.PHONY: installandroid
installandroid: ## Install the example Android app.
	./gradlew $(GRADLE_ARGS) examples:android:installDebug

.PHONY: licenseheaders
licenseheaders: $(BIN)/license-headers ## Format all files, adding license headers.
	comm -23 \
		<(git ls-files --cached --modified --others --no-empty-directory --exclude-standard | sort -u ) \
		<(git ls-files --deleted | sort -u) | \
		xargs $(BIN)/license-header \
			--license-type "apache" \
			--copyright-holder "Buf Technologies, Inc." \
			--year-range "$(LICENSE_HEADER_YEAR_RANGE)"

.PHONY: lint
lint: ## Run lint.
	buf lint
	./gradlew $(GRADLE_ARGS) spotlessCheck

.PHONY: lintfix
lintfix: # Applies the lint changes.
	./gradlew $(GRADLE_ARGS) spotlessApply

.PHONY: release
release: ## Upload artifacts to Sonatype Nexus.
	./gradlew $(GRADLE_ARGS) --info publish --stacktrace --no-daemon --no-parallel
	./gradlew $(GRADLE_ARGS) --info closeAndReleaseRepository

.PHONY: releaselocal
releaselocal: ## Release artifacts to local maven repository.
	./gradlew $(GRADLE_ARGS) --info publishToMavenLocal

.PHONY: test
test: generate ## Run tests for the library.
	./gradlew $(GRADLE_ARGS) library:test
