package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStrings")
    UserResponse toResponse(User user);

    @Named("rolesToStrings")
    default Set<String> rolesToStrings(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
            .map(r -> r.getName().name())
            .collect(Collectors.toSet());
    }
}
