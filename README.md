# 🚦 Circuit Breaker를 활용하여 MSA 환경에서 장애 전파 막기 🚦

서비스 하나에서 일어난 장애가 어떻게 다른 서비스로 장애가 전파될까?

그렇다면 우리는 이러한 현상을 막기 위한 전략들로 어떤 것들이 있을까?

[정리글]: [MSA 환경에서 장애 전파를 막기 위한 전략 (Circuit Breaker, Rate Limit)](https://jhzlo.tistory.com/75)

---



## 🧐 Circuit Breaker 코드 실습
### 1️⃣ 불안정한 서버 가정
![image](https://github.com/user-attachments/assets/64ea2dfe-0581-482a-9ad8-bc2bbf59a061)

예를 들어 위의 그림과 같이 `client <-> server`가 통신하고 있다고 가정한다.
```javascript
if (random < 0.2) {
                // 20% 확률로 500 에러
                res.writeHead(500, { 'Content-Type': 'text/plain' });
                return res.end('Internal Server Error');
            } else if (random < 0.3) {
                // 10% 확률로 400 에러
                res.writeHead(400, { 'Content-Type': 'text/plain' });
              return res.end('   Bad Request');
            }
```
이때, 서버는 다음과 같은 조건을 가지고 있다.

- `20% 확률`로 500 ERROR를 반환한다.
- `10% 확률`로 400 ERROR를 반환한다.

만약 서버에 장애가 일어났다면 쓰레드 고갈 방지를 위해 Client에서는 해당 서버에 대한 `Fail-Fast`가 이루어져야 한다.

---

### 2️⃣ Circuit Breaker 미들웨어 도입
서버에 장애가 일어났을 때 경우의 수는 **두 가지**이다.
1. 바로 클라이언트에게 에러메시지를 던져준다.
2. 클라이언트가 타임아웃 될 때까지 서버의 응답을 기다린다.

이때, **후자의 경우는 쓰레드 고갈로 이어져 다른 서버로 장애가 전파**될 수 있다.

![image](https://github.com/user-attachments/assets/c0c9f045-956d-4ce0-896b-9b5240e87b8c)

따라서 클라이언트와 서버 사이에 Circuit Breaker를 둚으로써 Circuit Breaker Pattern을 도입하여 해당 서버에 대한 `Fail-Fast`를 달성할 수 있다.

### 💡 Circut Breaker Pattern
Circuit Breaker Pattern은 다음과 같은 **세 가지의 상태**를 가지고 있다.

![image](https://github.com/user-attachments/assets/d31fe735-48b9-4348-8ed4-e7062c774d10)

1. `open` : `차단 상태`, 서비스 실패율이 설정된 임계값을 초과하면 서킷이 열리고, 추가적인 요청을 차단하여 시스템을 보호한다
2. `closed`: `정상 상태`, 서비스 요청이 정상적으로 처리되며, 실패율이 낮은 경우 서킷 브레이커는 닫힌 상태를 유지한다.
3. `half-open`: `테스트 상태`, open 상태 이후 일정 시간이 지난 후, 서킷이 반쯤 열려 일부 요청을 허용한다. 만약 요청이 성공하면 다시 Closed 상태로 전환하고, 실패하면 Open 상태를 유지

```yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10 # 최근 요청 10회
        failureRateThreshold: 15 # 실패율이 15% 넘는 경우에 open
        permittedNumberOfCallsInHalfOpenState: 5 # Half-Open 상태에서 허용할 요청 개수
        registerHealthIndicator: true  # Actuator의 Health Indicator 활성화
        minimumNumberOfCalls: 5  # Circuit Breaker가 활성화되기 위한 최소 요청 개수
        waitDurationInOpenState: 5s  # Open 상태 후 Half-Open으로 전환되기까지 대기 시간
        automaticTransitionFromOpenToHalfOpenEnabled: true  # Open 상태에서 자동으로 Half-Open 상태로 이동
        ignoreExceptions: # 400 에러는 서킷 브레이커에서 무시
          - org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
        recordExceptions: # 500 에러는 서킷 브레이커 조건으로 사용
          - org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
```
AOP의 방식으로 resilience4j를 이용하여 circuitbreaker의 서비스 실패율 임계값(15%)을 위와 같이 설정하였다.

![image](https://github.com/user-attachments/assets/be525c90-6467-444a-924d-d2b8d4c30196)

또한, 500에러에 대해서만 서킷 브레이커의 open 조건으로 이용하고 400 에러는 서킷 브레이커의 조건으로 사용하지 않도록 설정하였다.

![image](https://github.com/user-attachments/assets/56469aae-324f-4b78-b55a-e206a46b0bfe)

최근 요청 10회 동안 500 에러가 1.5번 이상 발생한 경우 Circut Breaker가 `open`상태가 되어 이후의 5초 (open상태 이후 half-open로 전환되기까지의 시간) 동안은 클라이언트 요청에 즉시 예외 메시지(`CallNotPermittedException`)를 던진다.

![image](https://github.com/user-attachments/assets/79a3c389-2c0f-4154-809d-f4413226f88d)

그리고 5초 후에 `half-open` 상태가 되어 minimumNumberOfCalls의 값인 5회의 정상적인 요청이 된다면 다시 `closed`의 상태로 바뀌어 기존의 server에 정상적으로 접근이 가능해진다.

---

### 3️⃣ Circut Breaker Service

```java
@CircuitBreaker(name = "default", fallbackMethod = "fallbackResponse")
    fun fetchData() {
        webClient.get()
            .uri(API)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        RequestStats.incrementSuccess()
    }

    private fun fallbackResponse(exception: WebClientResponseException) {
        when (exception.statusCode.value()) {
            400 -> RequestStats.incrementBadRequest()
            500 -> RequestStats.incrementInternalServerError()
        }
    }

    private fun fallbackResponse(exception: CallNotPermittedException) {
        RequestStats.incrementCircuitBreakerBlocked()
        logger.warn("Circuit Breaker is OPEN. Request blocked.")
    }

    private fun fallback(throwable: Throwable) {
        logger.warn("Fallback method triggered due to: ${throwable.message}")
    }
```

WebClient를 통해 `localhost:10001/api/random-error`에 접속할 때 fetchDate() 메서드를 통해서 비즈니스 로직이 실행되는데, 이때 발생하는 예외들은 fallbackMethod로 분기된다.

이때 발생하는 예외들의 종류에 따라 다음과 같이 처리된다.

- `4xx ERROR` => `WebClientResponseException`, RequestStats.badRequest ++
- `5xx ERROR` => `WebClientResponseException`, RequestStats.internalServerError ++
    - 5xx ERROR는 CircuitBreaker의 open 조건으로 사용된다.
- `CircuitBreaker OPEN` => `CallNotPermittedException`, RequestStats.circuitBreakerBlocked ++
- `이외 정상 처리` => RequestStats.success ++

---

### 4️⃣ k6 부하 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 1,  // 동시 사용자 수
    iterations: 1000, // 총 1000번 요청
};

export default function () {
    let testUrl = `http://localhost:8080/client/test`;

    let response = http.get(testUrl);

    check(response, {
        'status is 200': (r) => r.status === 200
    });

    sleep(0.1);
}

export function teardown() {
    let statsUrl = `http://localhost:8080/client/stats`;
    let statsResponse = http.get(statsUrl);

    console.log(`[STATS RESPONSE] ${statsResponse.body}`);
}
```
다음과 같은 k6 스크립트를 통해 테스트를 진행하였다. (1000번의 API 호출)

<img width="1121" alt="image" src="https://github.com/user-attachments/assets/9d4e6a77-5c9b-4554-89b7-d9fa1c990328" />

테스트를 진행한 결과 위의 로그와 같이 정상적으로 circuit이 `open` -> `half-open` -> `closed`의 상태를 변이하는 것을 살펴볼 수 있다.

다음은 실패율에 따른 `400 ERROR`, `500 ERROR`, `Circuit Blocked`, `정상처리` 의 비율을 확인하고자 한다.

### 🚨 실패율 15%
```yml
failureRateThreshold: 15
```

<img width="877" alt="image" src="https://github.com/user-attachments/assets/a1a18430-9879-4823-9821-803e2ce4d3e3" />

### 🚨 실패율 20%
```yml
failureRateThreshold: 20
```

<img width="790" alt="image" src="https://github.com/user-attachments/assets/8b01a45e-fa8d-41f5-b93e-c9d67e4849a3" />


### 🚨 실패율 25%
```yml
failureRateThreshold: 25
```

<img width="829" alt="image" src="https://github.com/user-attachments/assets/0024cb1e-a255-494b-b082-936532115c72" />

<br>
<br>
<br>

##### 📢REF
https://resilience4j.readme.io/docs/circuitbreaker

https://martinfowler.com/bliki/CircuitBreaker.html

