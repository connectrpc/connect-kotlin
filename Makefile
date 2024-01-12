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
LICENSE_HEADER_VERSION := v1.28.1
CONFORMANCE_VERSION := v1.0.0-rc2
PROTOC_VERSION ?= 25.1
GRADLE_ARGS ?=
PROTOC := $(BIN)/protoc
CONNECT_CONFORMANCE := $(BIN)/connectconformance

UNAME_OS := $(shell uname -s)
UNAME_ARCH := $(shell uname -m)

.PHONY: all
all: build

$(BIN)/license-headers: Makefile
	mkdir -p $(@D)
	GOBIN=$(abspath $(BIN)) go install github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@$(LICENSE_HEADER_VERSION)

.PHONY: build
build: generate ## Build the entire project.
	./gradlew $(GRADLE_ARGS) build

.PHONY: buildplugin
buildplugin: ## Build the connect-kotlin protoc plugin.
	./gradlew $(GRADLE_ARGS) protoc-gen-connect-kotlin:installDist

.PHONY: clean
clean: ## Cleans the underlying build.
	./gradlew $(GRADLE_ARGS) clean

# TODO: remove the cross-tests and rely solely on new conformance suite
.PHONY: runconformance
runconformance: runcrosstests runconformancenew

.PHONY: runconformancenew
runconformancenew: generate $(CONNECT_CONFORMANCE) ## Run the new conformance test suite.
	./gradlew $(GRADLE_ARGS) conformance:client:google-java:installDist conformance:client:google-javalite:installDist
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/lite-unary-config.yaml \
		--known-failing conformance/client/known-failing-unary-cases.txt -- \
		conformance/client/google-javalite/build/install/google-javalite/bin/google-javalite \
		--style suspend
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/lite-unary-config.yaml \
		--known-failing conformance/client/known-failing-unary-cases.txt -- \
		conformance/client/google-javalite/build/install/google-javalite/bin/google-javalite \
		--style callback
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/lite-unary-config.yaml \
		--known-failing conformance/client/known-failing-unary-cases.txt -- \
		conformance/client/google-javalite/build/install/google-javalite/bin/google-javalite \
		--style blocking
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/lite-stream-config.yaml \
		--known-failing conformance/client/known-failing-stream-cases.txt -- \
		conformance/client/google-javalite/build/install/google-javalite/bin/google-javalite
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/standard-unary-config.yaml \
		--known-failing conformance/client/known-failing-unary-cases.txt -- \
		conformance/client/google-java/build/install/google-java/bin/google-java \
		--style suspend
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/standard-unary-config.yaml \
		--known-failing conformance/client/known-failing-unary-cases.txt -- \
		conformance/client/google-java/build/install/google-java/bin/google-java \
		--style callback
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/standard-unary-config.yaml \
		--known-failing conformance/client/known-failing-unary-cases.txt -- \
		conformance/client/google-java/build/install/google-java/bin/google-java \
		--style blocking
	$(CONNECT_CONFORMANCE) -v --mode client --conf conformance/client/standard-stream-config.yaml \
		--known-failing conformance/client/known-failing-stream-cases.txt -- \
		conformance/client/google-java/build/install/google-java/bin/google-java

.PHONY: runcrosstests
runcrosstests: generate ## Run the old cross-test suite.
	./gradlew $(GRADLE_ARGS) conformance:google-java:test conformance:google-javalite:test

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

PROTOC_DOWNLOAD := $(CACHE)/protoc-$(PROTOC_VERSION).zip
$(PROTOC_DOWNLOAD):
	@if ! command -v curl >/dev/null 2>/dev/null; then echo "error: curl must be installed" >&2; exit 1; fi
	@if ! command -v unzip >/dev/null 2>/dev/null; then echo "error: unzip must be installed" >&2; exit 1; fi
	$(eval PROTOC_TMP := $(shell mktemp -d))
	curl -sSL https://github.com/protocolbuffers/protobuf/releases/download/v$(PROTOC_VERSION)/protoc-$(PROTOC_VERSION)-$(PROTOC_OS)-$(PROTOC_ARCH).zip -o $(PROTOC_TMP)/protoc.zip
	@mkdir -p $(dir $@)
	@mv $(PROTOC_TMP)/protoc.zip $@
	@rm -rf $(PROTOC_TMP)

$(PROTOC): $(PROTOC_DOWNLOAD)
	@mkdir -p $(BIN)
	unzip -DD -q $(PROTOC_DOWNLOAD) -d $(dir $(BIN)) bin/protoc
	chmod u+w $@

CONNECT_CONFORMANCE_DOWNLOAD := $(CACHE)/connect-conformance-$(CONFORMANCE_VERSION).tgz
$(CONNECT_CONFORMANCE_DOWNLOAD):
	@if ! command -v curl >/dev/null 2>/dev/null; then echo "error: curl must be installed" >&2; exit 1; fi
	@if ! command -v tar >/dev/null 2>/dev/null; then echo "error: tar must be installed" >&2; exit 1; fi
	$(eval CONFORMANCE_TMP := $(shell mktemp -d))
	curl -sSL https://github.com/connectrpc/conformance/releases/download/$(CONFORMANCE_VERSION)/connectconformance-$(CONFORMANCE_VERSION)-$(UNAME_OS)-$(UNAME_ARCH).tar.gz -o $(CONFORMANCE_TMP)/conformance.tgz
	@mkdir -p $(dir $@)
	@mv $(CONFORMANCE_TMP)/conformance.tgz $@
	@rm -rf $(CONFORMANCE_TMP)

$(CONNECT_CONFORMANCE): $(CONNECT_CONFORMANCE_DOWNLOAD)
	@mkdir -p $(BIN)
	tar -x -z -f $(CONNECT_CONFORMANCE_DOWNLOAD) -O connectconformance > $@
	@chmod +x $@

.PHONY: generate
generate: $(PROTOC) buildplugin generateconformance generateexamples ## Generate proto files for the entire project.
	buf generate --template protoc-gen-connect-kotlin/buf.gen.yaml -o protoc-gen-connect-kotlin protoc-gen-connect-kotlin/proto
	buf generate --template extensions/buf.gen.yaml -o extensions buf.build/googleapis/googleapis
	make licenseheaders

.PHONY: generateconformance
generateconformance: $(PROTOC) buildplugin ## Generate protofiles for conformance tests.
	buf generate --template conformance/buf.gen.yaml -o conformance conformance/proto
	buf generate --template conformance/buf.gen.yaml -o conformance/client buf.build/connectrpc/conformance:$(CONFORMANCE_VERSION)

.PHONY: generateexamples
generateexamples: $(PROTOC) buildplugin ## Generate proto files for example apps.
	buf generate --template examples/buf.gen.yaml -o examples buf.build/connectrpc/eliza

.PHONY: help
help: ## Describe useful make targets.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-30s %s\n", $$1, $$2}'

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
			--copyright-holder "The Connect Authors" \
			--year-range "$(LICENSE_HEADER_YEAR_RANGE)"

.PHONY: lint
lint: ## Run lint.
	buf lint
	./gradlew $(GRADLE_ARGS) spotlessCheck

.PHONY: lintfix
lintfix: ## Applies the lint changes.
	./gradlew $(GRADLE_ARGS) spotlessApply

.PHONY: release
release: generate ## Upload artifacts to Sonatype Nexus.
	./gradlew $(GRADLE_ARGS) --info publish --stacktrace --no-daemon --no-parallel
	./gradlew $(GRADLE_ARGS) --info closeAndReleaseRepository

.PHONY: releaselocal
releaselocal: ## Release artifacts to local maven repository.
	./gradlew $(GRADLE_ARGS) --info publishToMavenLocal

.PHONY: test
test: generate ## Run tests for the library.
	./gradlew $(GRADLE_ARGS) library:test
