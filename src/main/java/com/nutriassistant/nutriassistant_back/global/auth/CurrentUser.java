package com.nutriassistant.nutriassistant_back.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 현재 로그인한 사용자 정보를 주입하는 어노테이션
 *
 * 사용 예시:
 * <pre>
 * @GetMapping("/boards/{id}")
 * public ResponseEntity<?> getBoard(@CurrentUser UserContext user, @PathVariable Long id) {
 *     // user.getUserId(), user.getSchoolId() 사용 가능
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
