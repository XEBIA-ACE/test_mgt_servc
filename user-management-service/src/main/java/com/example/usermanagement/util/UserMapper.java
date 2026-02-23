package com.example.usermanagement.util;

import com.example.usermanagement.model.dto.response.UserResponse;
import com.example.usermanagement.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link User} entity and {@link UserResponse} DTO.
 *
 * <p>The {@code displayUsername} source overrides the default {@code getUsername()}
 * mapping, which returns the email address (required by Spring Security's
 * {@code UserDetails} contract).</p>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "displayUsername", target = "username")
    UserResponse toUserResponse(User user);
}
