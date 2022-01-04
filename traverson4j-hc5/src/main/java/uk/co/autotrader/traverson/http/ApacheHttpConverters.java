package uk.co.autotrader.traverson.http;


import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import uk.co.autotrader.traverson.conversion.ResourceConversionService;
import uk.co.autotrader.traverson.exception.ConversionException;
import uk.co.autotrader.traverson.http.entity.BodyFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ApacheHttpConverters {
    private static final AuthScope AUTH_SCOPE_MATCHING_ANYTHING = new AuthScope(null, null, -1, null, null);
    private final BodyFactory bodyFactory;
    private final TemplateUriUtils templateUriUtils;
    private final ResourceConversionService conversionService;

    public ApacheHttpConverters(BodyFactory bodyFactory, TemplateUriUtils templateUriUtils, ResourceConversionService conversionService) {
        this.bodyFactory = bodyFactory;
        this.templateUriUtils = templateUriUtils;
        this.conversionService = conversionService;
    }

    public ClassicHttpRequest toRequest(Request request) {
        Map<String, List<String>> templateParams = request.getTemplateParams();
        String uri = templateUriUtils.expandTemplateUri(request.getUrl(), templateParams);

        ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(request.getMethod().name()).setUri(uri);

        request.getQueryParameters().forEach((key, values) -> values.forEach(value -> requestBuilder.addParameter(key, value)));

        request.getHeaders().forEach(requestBuilder::addHeader);

        if (request.getAcceptMimeType() != null) {
            requestBuilder.addHeader("Accept", request.getAcceptMimeType());
        }

        Body body = request.getBody();
        if (body != null) {
            requestBuilder.setEntity(bodyFactory.toEntity(body));
        }
        return requestBuilder.build();
    }


    public <T> Response<T> toResponse(CloseableHttpResponse httpResponse, Class<T> returnType, URI uri) throws IOException {
        Response<T> response = new Response<>();
        response.setUri(uri);
        response.setStatusCode(httpResponse.getCode());
        Arrays.asList(httpResponse.getHeaders()).forEach(header ->
                response.addResponseHeader(header.getName(), header.getValue()));

        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                response.setResource(conversionService.convert(httpEntity.getContent(), returnType));
            } else {
                try {
                    response.setError(conversionService.convert(httpEntity.getContent(), String.class));
                } catch (ConversionException ignore) {

                }
            }
        }
        return response;
    }

    void constructCredentialsProviderAndAuthCache(BasicCredentialsProvider credentialsProvider, AuthCache authCache, AuthCredential authCredential) {
        UsernamePasswordCredentials userPassword = new UsernamePasswordCredentials(authCredential.getUsername(), authCredential.getPassword().toCharArray());
        AuthScope authScope = AUTH_SCOPE_MATCHING_ANYTHING;
        if (authCredential.getHostname() != null) {
            HttpHost target;
            try {
                target = HttpHost.create(authCredential.getHostname());
                authScope = new AuthScope(target);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Preemptive authentication hostname is invalid", e);
            }

            if (authCredential.isPreemptiveAuthentication()) {
                BasicScheme authScheme = new BasicScheme();
                authScheme.initPreemptive(userPassword);
                authCache.put(target, authScheme);
            }
        }
        credentialsProvider.setCredentials(authScope, userPassword);
    }

    HttpClientContext toHttpClientContext(Request request) {
        HttpClientContext clientContext = HttpClientContext.create();
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        AuthCache authCache = new BasicAuthCache();

        for (AuthCredential authCredential : request.getAuthCredentials()) {
            constructCredentialsProviderAndAuthCache(credentialsProvider, authCache, authCredential);
        }

        clientContext.setCredentialsProvider(credentialsProvider);
        clientContext.setAuthCache(authCache);
        return clientContext;
    }
}