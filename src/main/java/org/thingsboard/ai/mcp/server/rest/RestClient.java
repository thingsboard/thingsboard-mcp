package org.thingsboard.ai.mcp.server.rest;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.ai.mcp.server.util.RestJsonConverter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isEmpty;

public class RestClient implements Closeable {

    private static final String TOKEN_HEADER_PARAM = "X-Authorization";
    private static final long AVG_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";
    private final LazyInitializer<ExecutorService> executor = LazyInitializer.<ExecutorService>builder()
            .setInitializer(() -> ThingsBoardExecutors.newWorkStealingPool(10, getClass()))
            .get();
    @Getter
    protected final RestTemplate restTemplate;
    protected final RestTemplate loginRestTemplate;
    protected final String baseURL;

    private String username;
    private String password;
    private String mainToken;
    @Getter
    private String refreshToken;
    private long mainTokenExpTs;
    private long refreshTokenExpTs;
    private long clientServerTimeDiff;

    public enum AuthType {JWT, API_KEY}

    public RestClient(String baseURL) {
        this(new RestTemplate(), baseURL);
    }

    public RestClient(RestTemplate restTemplate, String baseURL) {
        this(restemplate, baseURL, AuthType.JWT, null);
    }

    public RestClient(RestTemplate restTemplate, String baseURL, AuthType authType, String token) {
        this.restTemplate = restTemplate;
        this.loginRestTemplate = new RestTemplate(restTemplate.getRequestFactory());
        this.baseURL = baseURL;
        this.restTemplate.getInterceptors().add((request, bytes, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            switch (authType) {
                case JWT -> {
                    if (token == null) {
                        long calculatedTs = System.currentTimeMillis() + clientServerTimeDiff + AVG_REQUEST_TIMEOUT;
                        if (calculatedTs > mainTokenExpTs) {
                            synchronized (RestClient.this) {
                                if (calculatedTs > mainTokenExpTs) {
                                    if (calculatedTs < refreshTokenExpTs) {
                                        refreshToken();
                                    } else {
                                        doLogin();
                                    }
                                }
                            }
                        }
                    } else {
                        mainToken = token;
                    }
                    wrapper.getHeaders().set(TOKEN_HEADER_PARAM, "Bearer " + mainToken);
                }
                case API_KEY -> {
                    wrapper.getHeaders().set(TOKEN_HEADER_PARAM, "ApiKey " + token);
                }
            }
            return execution.execute(wrapper, bytes);
        });
    }

    public static RestClient withApiKey(String baseURL, String token) {
        return new RestClient(new RestTemplate(), baseURL, AuthType.API_KEY, token);
    }

    public String getToken() {
        return mainToken;
    }

    public void refreshToken() {
        Map<String, String> refreshTokenRequest = new HashMap<>();
        refreshTokenRequest.put("refreshToken", refreshToken);
        long ts = System.currentTimeMillis();
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/token", refreshTokenRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    public void login(String username, String password) {
        this.username = username;
        this.password = password;
        doLogin();
    }

    private void doLogin() {
        long ts = System.currentTimeMillis();
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    private synchronized void setTokenInfo(long ts, JsonNode tokenInfo) {
        this.mainToken = tokenInfo.get("token").asText();
        this.refreshToken = tokenInfo.get("refreshToken").asText();
        this.mainTokenExpTs = JWT.decode(this.mainToken).getExpiresAtAsInstant().toEpochMilli();
        this.refreshTokenExpTs = JWT.decode(refreshToken).getExpiresAtAsInstant().toEpochMilli();
        this.clientServerTimeDiff = JWT.decode(this.mainToken).getIssuedAtAsInstant().toEpochMilli() - ts;
    }

    public Optional<AdminSettings> getAdminSettings(String key) {
        try {
            ResponseEntity<AdminSettings> adminSettings = restTemplate.getForEntity(baseURL + "/api/admin/settings/{key}", AdminSettings.class, key);
            return Optional.ofNullable(adminSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/settings", adminSettings, AdminSettings.class).getBody();
    }

    public void sendTestMail(AdminSettings adminSettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testMail", adminSettings);
    }

    public void sendTestSms(TestSmsRequest testSmsRequest) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testSms", testSmsRequest);
    }

    public Optional<SecuritySettings> getSecuritySettings() {
        try {
            ResponseEntity<SecuritySettings> securitySettings = restTemplate.getForEntity(baseURL + "/api/admin/securitySettings", SecuritySettings.class);
            return Optional.ofNullable(securitySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/securitySettings", securitySettings, SecuritySettings.class).getBody();
    }

    public Optional<JwtSettings> getJwtSettings() {
        try {
            ResponseEntity<JwtSettings> jwtSettings = restTemplate.getForEntity(baseURL + "/api/admin/jwtSettings", JwtSettings.class);
            return Optional.ofNullable(jwtSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public JwtPair saveJwtSettings(JwtSettings jwtSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/jwtSettings", jwtSettings, JwtPair.class).getBody();
    }

    public Optional<RepositorySettings> getRepositorySettings() {
        try {
            ResponseEntity<RepositorySettings> repositorySettings = restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings", RepositorySettings.class);
            return Optional.ofNullable(repositorySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean repositorySettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings/exists", Boolean.class).getBody();
    }

    public RepositorySettings saveRepositorySettings(RepositorySettings repositorySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/repositorySettings", repositorySettings, RepositorySettings.class).getBody();
    }

    public void deleteRepositorySettings() {
        restTemplate.delete(baseURL + "/api/admin/repositorySettings");
    }

    public void checkRepositoryAccess(RepositorySettings repositorySettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/repositorySettings/checkAccess", repositorySettings);
    }

    public Optional<AutoCommitSettings> getAutoCommitSettings() {
        try {
            ResponseEntity<AutoCommitSettings> autoCommitSettings = restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings", AutoCommitSettings.class);
            return Optional.ofNullable(autoCommitSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean autoCommitSettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings/exists", Boolean.class).getBody();
    }

    public AutoCommitSettings saveAutoCommitSettings(AutoCommitSettings autoCommitSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/autoCommitSettings", autoCommitSettings, AutoCommitSettings.class).getBody();
    }

    public void deleteAutoCommitSettings() {
        restTemplate.delete(baseURL + "/api/admin/autoCommitSettings");
    }

    public Optional<UpdateMessage> checkUpdates() {
        try {
            ResponseEntity<UpdateMessage> updateMsg = restTemplate.getForEntity(baseURL + "/api/admin/updates", UpdateMessage.class);
            return Optional.ofNullable(updateMsg.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> getSystemVersionInfo() {
        try {
            return Optional.ofNullable(restTemplate.getForEntity(baseURL + "/api/system/info", JsonNode.class).getBod…47704 tokens truncated…Template}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<IntegrationInfo>>() {
                }, params).getBody();
    }

    public void checkIntegrationConnection(Integration integration) {
        restTemplate.postForLocation(baseURL + "/api/integration/check", integration);
    }

    public void deleteIntegration(IntegrationId integrationId) {
        restTemplate.delete(baseURL + "/api/integration/{integrationId}", integrationId);
    }

    public List<Integration> getIntegrationsByIds(List<IntegrationId> integrationIds) {
        return restTemplate.exchange(
                baseURL + "/api/integrations?integrationIds={integrationIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Integration>>() {
                },
                listIdsToString(integrationIds)).getBody();
    }

    public Optional<Integration> assignIntegrationToEdge(EdgeId edgeId, IntegrationId integrationId) {
        try {
            ResponseEntity<Integration> integration = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/integration/{integrationId}",
                    null, Integration.class, edgeId.getId(), integrationId.getId());
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Integration> unassignIntegrationFromEdge(EdgeId edgeId, IntegrationId integrationId) {
        try {
            ResponseEntity<Integration> integration = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/integration/{integrationId}",
                    HttpMethod.DELETE, HttpEntity.EMPTY, Integration.class, edgeId.getId(), integrationId.getId());
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Integration> getEdgeIntegrations(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/integrations?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Integration>>() {
                }, params).getBody();
    }

    public PageData<IntegrationInfo> getEdgeIntegrationInfos(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/integrationInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<IntegrationInfo>>() {
                }, params).getBody();
    }

    public String findMissingAttributesForAllRelatedEdges(IntegrationId integrationId) {
        return restTemplate.getForEntity(baseURL + "/api/edge/integration/{integrationId}/allMissingAttributes", String.class, integrationId.getId()).getBody();
    }

    public String findMissingAttributesForEdge(EdgeId edgeId, List<IntegrationId> integrationIds) {
        return restTemplate.getForEntity(baseURL + "/api/edge/integration/{edgeId}/missingAttributes?integrationIds={integrationIds}",
                String.class, edgeId.getId(), listIdsToString(integrationIds)).getBody();
    }

    public void changeOwnerToTenant(EntityId ownerId, EntityId entityId) {
        changeOwnerToTenant(ownerId, entityId, new String[]{});
    }

    public void changeOwnerToTenant(EntityId ownerId, EntityId entityId, String[] strEntityGroupIds) {
        restTemplate.postForEntity(baseURL + "/api/owner/TENANT/{ownerId}/{entityType}/{entityId}", strEntityGroupIds, Object.class, ownerId.getId(), entityId.getEntityType(), entityId.getId());
    }

    public void changeOwnerToCustomer(EntityId ownerId, EntityId entityId) {
        changeOwnerToCustomer(ownerId, entityId, new String[]{});
    }

    public void changeOwnerToCustomer(EntityId ownerId, EntityId entityId, String[] strEntityGroupIds) {
        restTemplate.postForEntity(baseURL + "/api/owner/CUSTOMER/{ownerId}/{entityType}/{entityId}", strEntityGroupIds, Object.class, ownerId.getId(), entityId.getEntityType(), entityId.getId());
    }

    public Optional<Role> getRoleById(RoleId roleId) {
        try {
            ResponseEntity<Role> role = restTemplate.getForEntity(baseURL + "/api/role/{roleId}", Role.class, roleId.getId());
            return Optional.ofNullable(role.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Role saveRole(Role role) {
        return restTemplate.postForEntity(baseURL + "/api/role", role, Role.class).getBody();
    }

    public void deleteRole(RoleId roleId) {
        restTemplate.delete(baseURL + "/api/role/{roleId}", roleId.getId());
    }

    public PageData<Role> getRoles(RoleType type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type.name());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/roles?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Role>>() {
                }, params).getBody();
    }

    public List<Role> getRolesByIds(List<RoleId> roleIds) {
        return restTemplate.exchange(
                baseURL + "/api/roles?roleIds={roleIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Role>>() {
                },
                listIdsToString(roleIds)).getBody();
    }

    public Role createGroupRole(String roleName, List<Operation> operations) {
        Role role = new Role();
        role.setName(roleName);
        role.setType(RoleType.GROUP);
        ArrayNode permissions = JacksonUtil.newArrayNode();
        operations.stream().map(Operation::name).forEach(permissions::add);
        role.setPermissions(permissions);
        return saveRole(role);
    }

    public JsonNode handleRuleEngineRequest(JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                }).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId()).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, int timeout, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}/{timeout}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId(),
                timeout).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, String queueName, int timeout, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}/{queueName}/{timeout}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId(),
                queueName,
                timeout).getBody();
    }

    public Optional<SchedulerEventInfo> getSchedulerEventInfoById(SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEventInfo> schedulerEventInfo = restTemplate.getForEntity(baseURL + "/api/schedulerEvent/info/{schedulerEventId}", SchedulerEventInfo.class, schedulerEventId.getId());
            return Optional.ofNullable(schedulerEventInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SchedulerEvent> getSchedulerEventById(SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEvent> schedulerEvent = restTemplate.getForEntity(baseURL + "/api/schedulerEvent/{schedulerEventId}", SchedulerEvent.class, schedulerEventId.getId());
            return Optional.ofNullable(schedulerEvent.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        return restTemplate.postForEntity(baseURL + "/api/schedulerEvent", schedulerEvent, SchedulerEvent.class).getBody();
    }

    public void deleteSchedulerEvent(SchedulerEventId schedulerEventId) {
        restTemplate.delete(baseURL + "/api/schedulerEvent/{schedulerEventId}", schedulerEventId.getId());
    }

    public List<SchedulerEventInfo> getSchedulerEvents(String type) {
        return restTemplate.exchange(
                baseURL + "/api/schedulerEvents&type={type}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<SchedulerEventInfo>>() {
                },
                type).getBody();
    }

    public List<SchedulerEventInfo> getSchedulerEventsByIds(List<SchedulerEventId> schedulerEventIds) {
        return restTemplate.exchange(
                baseURL + "/api/schedulerEvents?schedulerEventIds={schedulerEventIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<SchedulerEventInfo>>() {
                },
                listIdsToString(schedulerEventIds)).getBody();
    }

    public SelfRegistrationParams saveSelfRegistrationParams(SelfRegistrationParams selfRegistrationParams) {
        return restTemplate.postForEntity(baseURL + "/api/selfRegistration/selfRegistrationParams", selfRegistrationParams, SelfRegistrationParams.class).getBody();
    }

    public Optional<SelfRegistrationParams> getSelfRegistrationParams() {
        try {
            ResponseEntity<SelfRegistrationParams> selfRegistrationParams = restTemplate.getForEntity(baseURL + "/api/selfRegistration/selfRegistrationParams}", SelfRegistrationParams.class);
            return Optional.ofNullable(selfRegistrationParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SignUpSelfRegistrationParams> getSignUpSelfRegistrationParams() {
        try {
            ResponseEntity<SignUpSelfRegistrationParams> selfRegistrationParams = restTemplate.getForEntity(baseURL + "/api/noauth/selfRegistration/signUpSelfRegistrationParams", SignUpSelfRegistrationParams.class);
            return Optional.ofNullable(selfRegistrationParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public String getPrivacyPolicy() {
        return restTemplate.getForEntity(baseURL + "/api/noauth/selfRegistration/privacyPolicy", String.class).getBody();
    }

    public SignUpResult signUp(SignUpRequest signUpRequest) {
        return restTemplate.postForEntity(baseURL + "/api/noauth/signup", signUpRequest, SignUpResult.class).getBody();
    }


    public void resendEmailActivation(String email) {
        restTemplate.postForEntity(baseURL + "/api/noauth/resendEmailActivation?email={email}", null, Object.class, email);
    }

    public ResponseEntity<String> activateEmail(String emailCode) {
        return restTemplate.exchange(
                baseURL + "/api/noauth/activateEmail?emailCode={emailCode}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseEntity<String>>() {
                },
                emailCode).getBody();
    }

    public Optional<JsonNode> activateUserByEmailCode(String emailCode) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activateByEmailCode?emailCode={emailCode}", null, JsonNode.class, emailCode);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean privacyPolicyAccepted() {
        return restTemplate.getForEntity(baseURL + "/api/signup/privacyPolicyAccepted", Boolean.class).getBody();
    }

    public Optional<JsonNode> acceptPrivacyPolicy() {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/signup/acceptPrivacyPolicy", null, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Optional<JsonNode> getLatestTimeseriesAsOptionalJson(EntityId entityId, String keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", keys);
        try {
            ResponseEntity<JsonNode> currentUserResponceEntity = restTemplate.getForEntity(baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}", JsonNode.class, params);
            return Optional.ofNullable(currentUserResponceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<Tenant> getTenantsByIds(List<TenantId> tenantIds) {
        return restTemplate.exchange(
                baseURL + "/api/tenants?tenantIds={tenantIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Tenant>>() {
                },
                listIdsToString(tenantIds)).getBody();
    }

    public PageData<User> getAllCustomerUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/customer/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                },
                params).getBody();
    }

    public PageData<User> getUserUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                },
                params).getBody();
    }

    public List<User> getUsersByIds(List<UserId> userIds) {
        return restTemplate.exchange(
                baseURL + "/api/users?userIds={userIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<User>>() {
                },
                listIdsToString(userIds)).getBody();
    }

    public Optional<AllowedPermissionsInfo> getAllowedPermissions() {
        try {
            ResponseEntity<AllowedPermissionsInfo> allowedPermissionsInfo = restTemplate.getForEntity(baseURL + "/api/permissions/allowedPermissions", AllowedPermissionsInfo.class);
            return Optional.ofNullable(allowedPermissionsInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WhiteLabelingParams> getWhiteLabelParams(String logoImageChecksum, String faviconChecksum) {
        try {
            ResponseEntity<WhiteLabelingParams> whiteLabelingParams =
                    restTemplate.getForEntity(
                            baseURL + "/api/whiteLabel/whiteLabelParams?logoImageChecksum={logoImageChecksum}&faviconChecksum={faviconChecksum}",
                            WhiteLabelingParams.class,
                            logoImageChecksum, faviconChecksum);
            return Optional.ofNullable(whiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<LoginWhiteLabelingParams> getLoginWhiteLabelParams(String logoImageChecksum, String faviconChecksum) {
        try {
            ResponseEntity<LoginWhiteLabelingParams> loginWhiteLabelingParams =
                    restTemplate.getForEntity(
                            baseURL + "/api/noauth/whiteLabel/loginWhiteLabelParams?logoImageChecksum={logoImageChecksum}&faviconChecksum={faviconChecksum}",
                            LoginWhiteLabelingParams.class,
                            logoImageChecksum,
                            faviconChecksum);
            return Optional.ofNullable(loginWhiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WhiteLabelingParams> getCurrentWhiteLabelParams() {
        try {
            ResponseEntity<WhiteLabelingParams> whiteLabelingParams =
                    restTemplate.getForEntity(baseURL + "/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
            return Optional.ofNullable(whiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<LoginWhiteLabelingParams> getCurrentLoginWhiteLabelParams() {
        try {
            ResponseEntity<LoginWhiteLabelingParams> loginWhiteLabelingParams =
                    restTemplate.getForEntity(baseURL + "/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
            return Optional.ofNullable(loginWhiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public WhiteLabelingParams saveWhiteLabelParams(WhiteLabelingParams whiteLabelingParams) {
        return restTemplate.postForEntity(baseURL + "/api/whiteLabel/whiteLabelParams",
                whiteLabelingParams,
                WhiteLabelingParams.class).getBody();
    }

    public void deleteWhiteLabelParams() {
        restTemplate.delete(baseURL + "/api/whiteLabel/currentWhiteLabelParams");
    }

    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/loginWhiteLabelParams",
                loginWhiteLabelingParams,
                LoginWhiteLabelingParams.class).getBody();
    }

    public void deleteLoginWhiteLabelParams() {
        restTemplate.delete(baseURL + "/api/whiteLabel/currentLoginWhiteLabelParams");
    }

    public WhiteLabelingParams previewWhiteLabelParams(WhiteLabelingParams whiteLabelingParams) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/previewWhiteLabelParams",
                whiteLabelingParams,
                WhiteLabelingParams.class).getBody();
    }

    public Boolean isWhiteLabelingAllowed() {
        return restTemplate.getForEntity(baseURL + "/api/whiteLabel/isWhiteLabelingAllowed", Boolean.class).getBody();
    }

    public Boolean isCustomerWhiteLabelingAllowed() {
        return restTemplate.getForEntity(
                baseURL + "/api/whiteLabel/isCustomerWhiteLabelingAllowed",
                Boolean.class).getBody();
    }

    public String getLoginThemeCss(PaletteSettings paletteSettings, boolean darkForeground) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/appThemeCss?darkForeground={darkForeground}",
                paletteSettings, String.class, darkForeground).getBody();
    }

    public String getAppThemeCss(PaletteSettings paletteSettings) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/appThemeCss",
                paletteSettings, String.class).getBody();
    }

    @SneakyThrows
    private ExecutorService getExecutor() {
        return executor.get();
    }

}
