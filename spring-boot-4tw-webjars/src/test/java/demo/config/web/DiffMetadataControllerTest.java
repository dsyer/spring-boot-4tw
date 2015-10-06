package demo.config.web;

import java.io.IOException;
import java.util.Arrays;

import demo.config.Application;
import demo.config.diff.ConfigDiffGenerator;
import demo.config.service.ConfigurationDiffResultLoader;
import demo.config.test.ConfigDiffResultTestLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class DiffMetadataControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@Before
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
	}

	@Test
	public void diffMetadataInvalidVersion() throws Exception {
		mockMvc.perform(
				get("/diff/1.3.0.RELEASE/foo-bar/").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError())
				.andExpect(mvcResult -> {
					Exception exception = mvcResult.getResolvedException();
					assertThat(exception, is(instanceOf(BindException.class)));
					BindException bindException = (BindException) exception;
					assertThat(bindException.getErrorCount(), equalTo(1));
					assertThat(bindException.getFieldError("toVersion")
							.getRejectedValue(), equalTo("foo-bar"));
				});
	}

	@Test
	public void diffInvalidVersion() throws Exception {
		mockMvc.perform(
				get("/diff/1.0.99.RELEASE/1.1.0.RELEASE/").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError())
				.andExpect(mvcResult -> {
					Exception exception = mvcResult.getResolvedException();
					assertThat(exception, is(instanceOf(BindException.class)));
					BindException bindException = (BindException) exception;
					assertThat(bindException.getErrorCount(), equalTo(1));
					FieldError error = bindException.getFieldError("fromVersion");
					assertThat(error.getRejectedValue(), equalTo("1.0.99.RELEASE"));
					assertThat(error.getCode(), equalTo("{Version.unknown}"));
				});
	}

	@Test
	public void diffMetadataHasETag() throws Exception {
		mockMvc.perform(
				get("/diff/1.0.1.RELEASE/1.1.0.RELEASE/").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is(200))
				.andExpect(header().string(HttpHeaders.ETAG, "\"1.0.1.RELEASE#1.1.0.RELEASE\""));
	}


	@Configuration
	static class MockConfiguration {

		@Bean
		public ConfigurationDiffResultLoader configurationDiffResultLoader() throws IOException {
			ConfigDiffGenerator diffGenerator = ConfigDiffResultTestLoader
					.mockConfigDiffGenerator(Arrays.asList("1.0.1.RELEASE", "1.1.0.RELEASE"),
							Arrays.asList("1.0.99.RELEASE", "1.1.89.RELEASE"));
			return new ConfigurationDiffResultLoader(diffGenerator);
		}

	}

}
