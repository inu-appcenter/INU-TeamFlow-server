# 코딩 컨벤션

## 기본 네이밍 규칙

- **클래스, 인터페이스**
    - PascalCase(`UserService`, `OrderController`)
- **메서드**
    - 메서드명은 동사로 시작한다.
    - camelCase(`findById` )
- **변수**
    - camelCase(`userName` )
    - **boolean 타입 변수명 작성**
        - is/has + 형용사/과거분사 (`isDeleted` , `hasPermission`)
- **상수**
    - UPPER_SNAKE_CASE(`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` )
- **패키지**
    - 언더스코어나 대문자를 섞지 않는다.
    - 소문자, 점 구분 (`com.example.project.domain.user` )
- **테스트 클래스는 'Test' 로 끝남**
    - `public class WatcherTest {`

### 메서드 네이밍 패턴

- 조회: `find`, `get` (`findById`, `findAll`, `getUserList`)
    - find: 없을 수 있음, Optional 반환
    - get: 반드시 존재, 없으면 예외 (getUserorThrow)
- 생성: `create`, `save` (`createUser`, `saveOrder`)
- 수정: `update` (`updateUserInfo`)
- 삭제: `delete` (`deleteById`)
- 존재 확인: `exists`, `is` (`existsById`, `isActive`)

## 선언

- **한 줄에 한 문장**
    - 문장이 끝나는 `;` 뒤에는 새줄을 삽입한다.
- **하나의 선언문에는 하나의 변수만**

    ```java
    int base;
    int weight;
    ```

## 들여쓰기

- **하드탭 사용**
    - 탭(tab) 문자를 사용해 들여쓴다. (스페이스 사용 x)
- **블럭 들여쓰기**
    - 클래서, 메서드, 제어문 등의 코드 블럭이 생길 때마다 1단계를 더 들여쓴다.

## 중괄호

- **닫는 중괄호와 같은 줄에 else, catch, finally, while 선언**
- **조건/반복문에 중괄호 필수 사용**
    - 한 줄로 끝이더라도 중괄호를 사용한다.

## 엔티티 작성 규칙

- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 사용
- 생성은 `@Builder`를 붙인 **private 생성자** + **정적 팩토리 메서드** 조합 사용. `public` 생성자 금지
- setter 사용하지 않기
    - 대신 수정 의도나 의미를 알 수 있는 메서드 작성

```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Builder
    private Team(String name) {
        this.name = name;
    }

    public static Team create(String name) {
        return Team.builder()
                .name(name)
                .build();
    }

    public void updateName(String name) {
        this.name = name;
    }
}
```

## DTO 작성 규칙

- **요청DTO**
    - 필드에 Bean Validation 어노테이션 사용 (@NotBlank, @Email, @Min 등)
    - 컨트롤러 파라미터에 @Valid 를 붙여 검증 활성화
- **응답DTO**
    - Entity 직접 반환 금지

## 서비스 레이어 규칙

- 클래스 레벨에 `@Transactional(readOnly = true)`를 기본으로 걸고, 쓰기 작업 메서드에만 `@Transactional`을 오버라이드
- Repository 는 Service 에서만 접근 (Controller 직접 접근 금지)

## 컨트롤러 레이어 규칙

- `@RestController` + `@RequestMapping` 사용
- 응답은 공통 `ApiResponse<T>` 래퍼로 통일
- 요청 DTO 파라미터에 @Valid

## 예외 처리 규칙

- 커스텀 예외는 `RuntimeException` 상속, 도메인별로 정의
- `@RestControllerAdvice`로 전역 예외 핸들링
- 예외 메시지는 사용자에게 노출될 수 있으므로 신중하게 작성

## 디렉토리 구조

도메인 중심의 패키지 구조를 따른다.

```
com.inu.teamflow.server
├── global
│   ├── config
│   ├── exception
│   ├── jwt
│   └── s3
└── domain
    ├── user
    │   ├── controller
    │   ├── service
    │   ├── repository
    │   ├── entity
    │   ├── enums
    │   └── dto
    │       ├── request
    │       └── response
    ├── team
    ├── event
    ├── recruitment
    └── invitation
```

- 횡단 관심사(설정, 예외, JWT, S3 등)는 `global` 패키지에 위치
- 비즈니스 로직은 도메인별로 `domain` 하위에 분리
- 각 도메인은 `controller / service / repository / entity / enums / dto` 구조를 기본으로 한다
