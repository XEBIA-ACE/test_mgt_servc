package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper — Spring component generated at compile time. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "enabled", source = "enabled")
    UserResponse toResponse(User user);
}
