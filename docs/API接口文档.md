# FISCO BCOS 供应链金融平台 API 文档

**版本**: 1.0.0
**描述**: 区块链供应链金融平台 API 文档

---

### authorization-test-controller

**GET** `/api/test/admin`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/async/quick`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/async/status/{taskId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| taskId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/async/submit`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/audit/entity`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/audit/entity/no-fill`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/audit/public`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/blockchain/record`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/blockchain/record/{txHash}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| txHash | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/blockchain/records/ent/{entId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/blockchain/records/user/{userId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/circuit-breaker/blockchain`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/circuit-breaker/fail`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/circuit-breaker/normal`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/encryption/aes/decrypt`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/encryption/aes/encrypt`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/encryption/rsa/publickey`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/error/business`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/error/throw`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/error/with-stack`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/finance`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/hello`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/idempotent/create`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/idempotent/create-optional`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/idempotent/no-check`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/info`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/debug`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/error`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/fatal`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/info`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/operation`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/transaction/{txHash}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| txHash | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/log/warn`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/mask/address/{address}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| address | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/mask/bankcard/{cardNumber}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| cardNumber | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/mask/email/{email}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| email | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/mask/idcard/{idCard}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| idCard | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/mask/phone/{phone}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| phone | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/naming/request`

**请求体:**

- Schema: `#/components/schemas/TokenResponseDTO`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/naming/response`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/ownership/asset/{ownerId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| ownerId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/ownership/enterprise/{entId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/ownership/public`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/ownership/strict/{entId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/perm-admin`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/perm-finance`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/perm-user-only`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/public`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/rate-limit/custom`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/rate-limit/query`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/rate-limit/write`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/response/error`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/response/page`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/response/success/data`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/response/success/null`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/status`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/timeout/fallback`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/timeout/normal`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/timeout/short`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**GET** `/api/test/user-only`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/validation/hex-address`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### authorization-test-controller

**POST** `/api/test/validation/hex-address/simple`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### auth-controller

**POST** `/api/v1/auth/login`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logout-controller

**POST** `/api/v1/auth/logout`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| Authorization | header | string | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### auth-controller

**POST** `/api/v1/auth/refresh`

**请求体:**

- Schema: `#/components/schemas/RefreshTokenRequestDTO`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### auth-controller

**POST** `/api/v1/auth/validate`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/account`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/balance/{address}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| address | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/block/{blockNumber}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| blockNumber | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/blockHash/{blockNumber}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| blockNumber | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/blockNumber`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**POST** `/api/v1/blockchain/call`

**请求体:**

- Schema: `#/components/schemas/ContractCallRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/group`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/groups`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/health`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/receipt/{txHash}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| txHash | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**GET** `/api/v1/blockchain/status`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-controller

**POST** `/api/v1/blockchain/transaction`

**请求体:**

- Schema: `#/components/schemas/ContractCallRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**GET** `/api/v1/credit/blacklist/check`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**DELETE** `/api/v1/credit/blacklist/remove`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**POST** `/api/v1/credit/blacklist/trigger`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**POST** `/api/v1/credit/event/logistics-deviation`

**请求体:**

- Schema: `#/components/schemas/LogisticsDeviationRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**POST** `/api/v1/credit/event/report`

**请求体:**

- Schema: `#/components/schemas/CreditEventReportRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**GET** `/api/v1/credit/events`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |
| eventType | query | string | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**PUT** `/api/v1/credit/limit`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| availableLimit | query | number | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**GET** `/api/v1/credit/limit/available`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**POST** `/api/v1/credit/limit/check`

**请求体:**

- Schema: `#/components/schemas/CreditLimitCheckRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**POST** `/api/v1/credit/limit/lock`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**GET** `/api/v1/credit/profile`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**GET** `/api/v1/credit/profile/me`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**PATCH** `/api/v1/credit/reevaluate`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**PATCH** `/api/v1/credit/reevaluate/batch`

**请求体:**

- Schema: `#/components/schemas/BatchRecalculateRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### credit-controller

**GET** `/api/v1/credit/score`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/admin/login`

**请求体:**

- Schema: `#/components/schemas/LoginRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/asset-balance`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/cancellation/apply`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 是 |  |
| reason | query | string | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/cancellation/pending`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/cancellation/revoke`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/chain/code/{creditCode}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| creditCode | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/chain/list`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/chain/{address}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| address | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/detail`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/get_user`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 是 |  |
| userId | query | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/info_user`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| enterpriseId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/invite-codes`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |
| maxUses | query | integer | 否 |  |
| expireDays | query | integer | 否 |  |
| remark | query | string | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/invite-codes/list`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**DELETE** `/api/v1/enterprise/invite-codes/{codeId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| codeId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/list`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| status | query | integer | 否 |  |
| entRole | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/login`

**请求体:**

- Schema: `#/components/schemas/LoginRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**PUT** `/api/v1/enterprise/password/login`

**请求体:**

- Schema: `#/components/schemas/PasswordUpdateRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**PUT** `/api/v1/enterprise/password/pay`

**请求体:**

- Schema: `#/components/schemas/PasswordUpdateRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/pending`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/register`

**请求体:**

- Schema: `#/components/schemas/EnterpriseRegisterRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**PUT** `/api/v1/enterprise/users/{userId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | query | integer | 是 |  |
| userId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/ManageUserRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**GET** `/api/v1/enterprise/{entId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/{entId}/audit`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/AuditRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**POST** `/api/v1/enterprise/{entId}/cancellation/audit`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/AuditRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**PUT** `/api/v1/enterprise/{entId}/credit-limit`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/CreditLimitRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**PUT** `/api/v1/enterprise/{entId}/rating`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/RatingRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### enterprise-controller

**PUT** `/api/v1/enterprise/{entId}/status`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/StatusRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**PATCH** `/api/v1/finance/receivable/adjust`

**请求体:**

- Schema: `#/components/schemas/AdjustReceivableRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**POST** `/api/v1/finance/receivable/confirm`

**请求体:**

- Schema: `#/components/schemas/ConfirmReceivableRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**GET** `/api/v1/finance/receivable/creditor/list`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**GET** `/api/v1/finance/receivable/debtor/list`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**GET** `/api/v1/finance/receivable/ent/{entId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| entId | path | integer | 是 |  |
| roleType | query | string | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**POST** `/api/v1/finance/receivable/finance`

**请求体:**

- Schema: `#/components/schemas/FinanceReceivableRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**POST** `/api/v1/finance/receivable/generate`

**请求体:**

- Schema: `#/components/schemas/GenerateReceivableRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**GET** `/api/v1/finance/receivable/no/{receivableNo}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receivableNo | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**POST** `/api/v1/finance/receivable/settle`

**请求体:**

- Schema: `#/components/schemas/SettleReceivableRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**GET** `/api/v1/finance/receivable/{id}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| id | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**POST** `/api/v1/finance/repayment/cash`

**请求体:**

- Schema: `#/components/schemas/CashRepaymentRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**GET** `/api/v1/finance/repayment/list/{receivableId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receivableId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### finance-controller

**POST** `/api/v1/finance/repayment/offset`

**请求体:**

- Schema: `#/components/schemas/OffsetWithCollateralRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### health-controller

**GET** `/api/v1/health`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/arrive`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/assign`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/create`

**请求体:**

- Schema: `#/components/schemas/LogisticsDelegate`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/delegate/list`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| businessScene | query | integer | 否 |  |
| status | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/delegate/{voucherNo}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| voucherNo | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/delivery/confirm`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/invalidate`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/pickup`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**PUT** `/api/v1/logistics/status`

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/track`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| voucherNo | query | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/track/deviations`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| voucherNo | query | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/track/latest`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| voucherNo | query | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/track/list`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| voucherNo | query | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**POST** `/api/v1/logistics/track/report`

**请求体:**

- Schema: `#/components/schemas/LogisticsTrack`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### logistics-controller

**GET** `/api/v1/logistics/validate`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| voucherNo | query | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-test-controller

**GET** `/api/v1/test/blockchain`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### blockchain-test-controller

**GET** `/api/v1/test/blockchain/health`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**PUT** `/api/v1/user/assign_role`

**请求体:**

- Schema: `#/components/schemas/AssignRoleRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/cancel/apply`

**请求体:**

- Schema: `#/components/schemas/CancellationApplyRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**GET** `/api/v1/user/cancel/pending`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| enterpriseId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/cancel/revoke`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**PUT** `/api/v1/user/disable/{userId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**GET** `/api/v1/user/list`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| enterpriseId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/login`

**请求体:**

- Schema: `#/components/schemas/LoginRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/password`

**请求体:**

- Schema: `#/components/schemas/PasswordUpdateRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**GET** `/api/v1/user/pending`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| enterpriseId | query | integer | 否 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**GET** `/api/v1/user/profile`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/register`

**请求体:**

- Schema: `#/components/schemas/UserRegisterRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**PUT** `/api/v1/user/status`

**请求体:**

- Schema: `#/components/schemas/UserStatusRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**PUT** `/api/v1/user/update`

**请求体:**

- Schema: `#/components/schemas/UserUpdateRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**DELETE** `/api/v1/user/{userId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**GET** `/api/v1/user/{userId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | path | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/{userId}/audit`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/AuditUserRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### user-controller

**POST** `/api/v1/user/{userId}/cancel/audit`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| userId | path | integer | 是 |  |

**请求体:**

- Schema: `#/components/schemas/AuditUserRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/burn/apply`

**请求体:**

- Schema: `#/components/schemas/ApplyBurnRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/burn/{stockOrderId}/confirm`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| stockOrderId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/endorsement/launch`

**请求体:**

- Schema: `#/components/schemas/LaunchEndorsementRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/endorsement/list`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receiptId | query | integer | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/endorsement/{endorsementId}/confirm`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| endorsementId | path | string | 是 |  |
| accept | query | boolean | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/endorsement/{endorsementId}/revoke`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| endorsementId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/merge/apply`

**请求体:**

- Schema: `#/components/schemas/ApplyMergeRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/receipt/by-chain/{onChainId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| onChainId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/receipt/in-stock`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/receipt/list`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/receipt/mint`

**请求体:**

- Schema: `#/components/schemas/MintReceiptRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/receipt/{receiptId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/receipt/{receiptId}/lock`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | 是 |  |

**请求体:**

- Type: `application/json`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/receipt/{receiptId}/trace`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/receipt/{receiptId}/unlock`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/split-merge/{opLogId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| opLogId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/split-merge/{opLogId}/execute`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| opLogId | path | string | 是 |  |
| execute | query | boolean | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/split/apply`

**请求体:**

- Schema: `#/components/schemas/ApplySplitRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/stock-in/apply`

**请求体:**

- Schema: `#/components/schemas/StockInApplyRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/stock-in/list`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/stock-in/{stockOrderId}`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| stockOrderId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/stock-in/{stockOrderId}/cancel`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| stockOrderId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/stock-in/{stockOrderId}/confirm`

**参数:**

| 名称 | 位置 | 类型 | 必需 | 描述 |
|------|------|------|------|------|
| stockOrderId | path | string | 是 |  |

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**POST** `/api/v1/warehouse/warehouse/create`

**请求体:**

- Schema: `#/components/schemas/CreateWarehouseRequest`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

### warehouse-receipt-controller

**GET** `/api/v1/warehouse/warehouse/list`

**响应:**

- `500`: Internal Server Error
- `400`: Bad Request
- `200`: OK

---

