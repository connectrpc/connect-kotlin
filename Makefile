# See https://tech.davis-hansson.com/p/make/
SHELL := bash
.DELETE_ON_ERROR:
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := all
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-print-directory
BIN := .tmp/bin
LICENSE_HEADER_YEAR_RANGE := 2022-2023
CROSSTEST_VERSION := 4f4e96d8fea3ed9473b90a964a5ba429e7ea5649
LICENSE_HEADER_VERSION := v1.12.0

$(BIN)/license-headers: Makefile
	mkdir -p $(@D)
	GOBIN=$(abspath $(BIN)) go install github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@$(LICENSE_HEADER_VERSION)

.PHONY: build
build: generate ## Build the entire project.
	./gradlew build

.PHONY: buildplugin
buildplugin: ## Build the connect-kotlin protoc plugin.
	./gradlew protoc-gen-connect-kotlin:jar

.PHONY: clean
clean: ## Cleans the underlying build.
	./gradlew clean
	rm -rf examples/generated-google-java/src/main
	rm -rf examples/generated-google-javalite/src/main

	rm -rf crosstests/google-java/src/main/java/generated
	rm -rf crosstests/google-java/src/main/kotlin/generated
	rm -rf crosstests/google-javalite/src/main/java/generated
	rm -rf crosstests/google-javalite/src/main/kotlin/generated

	rm -rf protoc-gen-connect-kotlin/generation/src/test/java/
	rm -rf protoc-gen-connect-kotlin/generation/src/test/kotlin/

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
crosstestsrun: crosstestsrunjava crosstestsrunjavalite ## Run the cross tests.

.PHONY: crosstestsrunjava
crosstestsrunjava: ## Run the cross tests for protoc-gen-java integration.
	./gradlew crosstests:google-java:jar
	java -jar ./crosstests/google-java/build/libs/google-java-crosstests.jar

.PHONY: crosstestsrunjavalite
crosstestsrunjavalite: ## Run the cross tests for protoc-gen-javalite integration.
	./gradlew crosstests:google-javalite:jar
	java -jar ./crosstests/google-javalite/build/libs/google-javalite-crosstests.jar

.PHONY: generate
generate: buildplugin generatecrosstests generateexamples ## Generate proto files for the entire project.
	buf generate --template protoc-gen-connect-kotlin/buf.gen.yaml -o protoc-gen-connect-kotlin
	buf generate --template extensions/buf.gen.yaml -o extensions buf.build/googleapis/googleapis
	make licenseheaders

.PHONY: generatecrosstests
generatecrosstests: buildplugin ## Generate protofiles for cross tests.
	buf generate --template crosstests/buf.gen.yaml -o crosstests

.PHONY: generateexamples
generateexamples: buildplugin ## Generate proto files for example apps.
	buf generate --template examples/buf.gen.yaml -o examples buf.build/bufbuild/eliza

.PHONY: help
help: ## Describe useful make targets.
	grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-30s %s\n", $$1, $$2}'

.PHONY: installandroid
installandroid: ## Install the example Android app.
	./gradlew examples:android:installDebug

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
	./gradlew spotlessCheck

.PHONY: lintfix
lintfix: # Applies the lint changes.
	./gradlew spotlessApply

.PHONY: release
release: ## Release artifacts to Sonatype Nexus.
	./gradlew --info publish --stacktrace --no-daemon --no-parallel
	./gradlew --info closeAndReleaseRepository

.PHONY: releaselocal
releaselocal: ## Release artifacts to local maven repository.
	./gradlew --info publishToMavenLocal

.PHONY: test
test: ## Run tests for the library.
	./gradlew library:test
