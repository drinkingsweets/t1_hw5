package org.example.t1_hw4.mapper;

import org.example.t1_hw4.dto.RegisterDTO;
import org.example.t1_hw4.dto.UserDTO;
import org.example.t1_hw4.model.User;
import org.mapstruct.*;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {

    @Mappings({
            @Mapping(source = "login", target = "login"),
            @Mapping(source = "email", target = "email"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "passwordDigest", ignore = true),
            @Mapping(source = "role", target = "role")
    })
    public abstract User toUser(RegisterDTO dto);

    public abstract UserDTO toUserDTO(User user);
}
