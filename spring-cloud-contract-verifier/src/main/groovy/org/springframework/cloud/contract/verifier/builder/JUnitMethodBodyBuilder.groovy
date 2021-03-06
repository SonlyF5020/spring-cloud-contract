/*
 *  Copyright 2013-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.contract.verifier.builder

import groovy.json.StringEscapeUtils
import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.spec.internal.ExecutionProperty
import org.springframework.cloud.contract.spec.internal.Header
import org.springframework.cloud.contract.spec.internal.NamedProperty
import org.springframework.cloud.contract.spec.internal.Request
import org.springframework.cloud.contract.verifier.config.ContractVerifierConfigProperties

import java.util.regex.Pattern

import static groovy.json.StringEscapeUtils.escapeJava
import static org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT
import static org.springframework.cloud.contract.verifier.util.ContentUtils.getJavaMultipartFileParameterContent

/**
 * Root class for JUnit method building
 *
 * Do not change to {@code @CompileStatic} since it's using double dispatch.
 *
 * @author Jakub Kubrynski, codearte.io
 * @author Olga Maciaszek-Sharma, codearte.io
 *
 * @since 1.0.0
 */
@TypeChecked
@PackageScope
abstract class JUnitMethodBodyBuilder extends RequestProcessingMethodBodyBuilder {

	JUnitMethodBodyBuilder(Contract stubDefinition, ContractVerifierConfigProperties configProperties) {
		super(stubDefinition, configProperties)
	}

	@Override
	protected String getResponseAsString() {
		return "response.getBody().asString()"
	}

	@Override
	protected String addCommentSignIfRequired(String baseString) {
		return "// $baseString"
	}

	@Override
	protected BlockBuilder addColonIfRequired(BlockBuilder blockBuilder) {
		blockBuilder.addAtTheEnd(JUNIT.lineSuffix)
		return blockBuilder
	}

	@Override
	protected String getResponseBodyPropertyComparisonString(String property, String value) {
		return "assertThat(responseBody${property}).isEqualTo(\"${value}\")"
	}

	@Override
	protected void processBodyElement(BlockBuilder blockBuilder, String property, ExecutionProperty exec) {
		blockBuilder.addLine("${exec.insertValue("parsedJson.read(\"\\\$$property\")")};")
	}

	@Override
	protected String getPropertyInListString(String property, Integer listIndex) {
		return "${property}.get($listIndex)" ?: ''
	}

	@Override
	protected String convertUnicodeEscapesIfRequired(String json) {
		String unescapedJson = StringEscapeUtils.unescapeJavaScript(json)
		return escapeJava(unescapedJson)
	}

	@Override
	protected void processBodyElement(BlockBuilder blockBuilder, String property, Map.Entry entry) {
		processBodyElement(blockBuilder, property + getMapKeyReferenceString(entry), entry.value)
	}

	private String getMapKeyReferenceString(Map.Entry entry) {
		if (entry.value instanceof ExecutionProperty) {
			return "." + entry.key
		}
		return """.get(\\\"$entry.key\\\")"""
	}

	@Override
	protected String getParsedXmlResponseBodyString(String responseString) {
		return "Object responseBody = new XmlSlurper().parseText($responseString);"
	}

	@Override
	protected String getSimpleResponseBodyString(String responseString) {
		return "Object responseBody = ($responseString);"
	}

	@Override
	protected String getInputString(Request request) {
		def inputString =  'ResponseOptions response = given().spec(request)'
		if (response.async){
			inputString = inputString + '.when().async()'
		}
		return inputString
	}

	@Override
	protected String getInputString() {
		return 'MockMvcRequestSpecification request = given()'
	}

	@Override
	protected String getHeaderString(Header header) {
		return ".header(\"${getTestSideValue(header.name)}\", \"${getTestSideValue(header.serverValue)}\")"
	}

	@Override
	protected String getBodyString(String bodyAsString) {
		return ".body(\"$bodyAsString\")"
	}

	@Override
	protected String getMultipartFileParameterContent(String propertyName, NamedProperty propertyValue) {
		return getJavaMultipartFileParameterContent(propertyName, propertyValue)
	}

	@Override
	protected String getParameterString(Map.Entry<String, Object> parameter) {
		return """.param("${escapeJava(parameter.key)}", "${escapeJava(parameter.value as String)}")"""
	}

	protected String createHeaderComparison(Object headerValue) {
		String escapedHeader = convertUnicodeEscapesIfRequired("$headerValue")
		return "isEqualTo(\"$escapedHeader\");"
	}

	protected String createHeaderComparison(Pattern headerValue) {
		String escapedHeader = convertUnicodeEscapesIfRequired("$headerValue")
		return "matches(\"$escapedHeader\");"
	}

}
