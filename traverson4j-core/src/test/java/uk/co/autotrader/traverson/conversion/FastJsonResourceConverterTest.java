package uk.co.autotrader.traverson.conversion;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.assertj.core.data.MapEntry;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.co.autotrader.traverson.exception.ConversionException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class FastJsonResourceConverterTest {

    private FastJsonResourceConverter converter;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        converter = new FastJsonResourceConverter();
    }

    @Test
    public void getDestinationType_ReturnsJSONObject() {
        assertThat(converter.getDestinationType()).isEqualTo(JSONObject.class);
    }

    @Test
    public void convert_GivenJsonString_ParsesJsonCorrectly() {
        String resourceAsString = "{'name':'test', 'anotherName':'comes before the first one alphabetically'}";

        JSONObject resource = converter.convert(IOUtils.toInputStream(resourceAsString, StandardCharsets.UTF_8), JSONObject.class);

        assertThat(resource).isNotNull().containsExactly(MapEntry.entry("name", "test"), MapEntry.entry("anotherName", "comes before the first one alphabetically"));
    }

    @Test
    public void convert_GivenXMLString_ThrowsConversionException() {
        final String resourceAsString = "<xml><_links><self><href>http://localhost</href></self></_links></xml>";
        final BaseMatcher<Object> matcher = new BaseMatcher<Object>() {
            @Override
            public void describeTo(Description description) { }
            @Override
            public boolean matches(Object item) {
                ConversionException ex = (ConversionException) item;
                return ex.getResourceAsString().equals(resourceAsString);
            }
        };

        expectedException.expect(ConversionException.class);
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(JSONException.class));
        expectedException.expect(matcher);

        converter.convert(IOUtils.toInputStream(resourceAsString, StandardCharsets.UTF_8), JSONObject.class);
    }

    @Test
    public void convert_GivenEmptyString_ReturnNull() {
        String resourceAsString = "";

        JSONObject resource = converter.convert(IOUtils.toInputStream(resourceAsString, StandardCharsets.UTF_8), JSONObject.class);

        assertThat(resource).isNull();
    }

    @Test
    public void convert_GivenNullString_ReturnNull() {
        String resourceAsString = "";

        JSONObject resource = converter.convert(IOUtils.toInputStream(resourceAsString, StandardCharsets.UTF_8), JSONObject.class);

        assertThat(resource).isNull();
    }
}
