package demo.config.web;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.web.context.WebApplicationContext;

import demo.config.Application;
import demo.config.diff.ConfigDiffGenerator;
import demo.config.diff.support.UnknownSpringBootVersion;
import demo.config.service.ConfigurationDiffResultLoader;
import demo.config.test.ConfigDiffResultTestLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@TestPropertySource(properties="spring.jackson.serialization.indent-output=true")
public class DiffMetadataControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@Rule
	public RestDocumentation restDocumentation = new RestDocumentation("target/generated-snippets");

	@Before
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
				.apply(documentationConfiguration(this.restDocumentation)).build();
	}

	@Test
	public void diffMetadataInvalidVersion() throws Exception {
		mockMvc.perform(get("/diff/1.3.0.RELEASE/foo-bar/").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError()).andExpect(mvcResult -> {
					Exception exception = mvcResult.getResolvedException();
					assertThat(exception, is(instanceOf(BindException.class)));
					BindException bindException = (BindException) exception;
					assertThat(bindException.getErrorCount(), equalTo(1));
					assertThat(bindException.getFieldError("toVersion").getRejectedValue(), equalTo("foo-bar"));
				}).andDo(document("to-invalid"));
	}

	@Test
	public void diffInvalidVersion() throws Exception {
		mockMvc.perform(get("/diff/1.0.99.RELEASE/1.1.0.RELEASE/").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError()).andExpect(mvcResult -> {
					Exception exception = mvcResult.getResolvedException();
					assertThat(exception, is(instanceOf(UnknownSpringBootVersion.class)));
					UnknownSpringBootVersion bindException = (UnknownSpringBootVersion) exception;
					assertThat(bindException.getVersion(), equalTo("1.0.99.RELEASE"));
				}).andDo(document("from-invalid"));
	}

	@Test
	public void diffMetadataHasETag() throws Exception {
		mockMvc.perform(get("/diff/1.0.1.RELEASE/1.1.0.RELEASE/").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is(200))
				.andExpect(header().string(HttpHeaders.ETAG, "\"1.0.1.RELEASE#1.1.0.RELEASE\""))
				.andDo(document("valid"));
	}

	@Configuration
	static class MockConfiguration {

		@Bean
		public ConfigurationDiffResultLoader configurationDiffResultLoader() throws IOException {
			ConfigDiffGenerator diffGenerator = ConfigDiffResultTestLoader.mockConfigDiffGenerator(
					Arrays.asList("1.0.1.RELEASE", "1.1.0.RELEASE"), Arrays.asList("1.0.99.RELEASE", "1.1.89.RELEASE"));
			return new ConfigurationDiffResultLoader(diffGenerator);
		}

	}

}
