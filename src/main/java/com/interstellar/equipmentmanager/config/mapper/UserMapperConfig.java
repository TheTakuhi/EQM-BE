package com.interstellar.equipmentmanager.config.mapper;

import com.interstellar.equipmentmanager.model.dto.contract.out.ContractCroppedDTO;
import com.interstellar.equipmentmanager.model.dto.keycloak.user.out.KeycloakUserDTO;
import com.interstellar.equipmentmanager.model.dto.team.out.TeamCroppedDTO;
import com.interstellar.equipmentmanager.model.dto.user.in.UserCreateDTO;
import com.interstellar.equipmentmanager.model.dto.user.out.UserCroppedDTO;
import com.interstellar.equipmentmanager.model.dto.user.out.UserDTO;
import com.interstellar.equipmentmanager.model.dto.user.in.UserEditDTO;
import com.interstellar.equipmentmanager.model.entity.Contract;
import com.interstellar.equipmentmanager.model.dto.request.LdapUser;
import com.interstellar.equipmentmanager.model.entity.Team;
import com.interstellar.equipmentmanager.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class UserMapperConfig {

    private final ModelMapper mapper;
    private final Converter<User, UUID> userToId = ctx -> ctx.getSource() == null ? null : ctx.getSource().getId();

    private final Converter<UUID, User> idToUser = ctx -> {
        if (ctx.getSource() == null) {
            return null;
        }
        var user = new User();
        user.setId(ctx.getSource());
        return user;
    };

    private final Converter<UserCreateDTO, String> fullName = ctx -> {
        if (ctx.getSource() == null) {
            return null;
        }
        return ctx.getSource().getFirstName() + ctx.getSource().getLastName();
    };

    private final Converter<UserDTO, UUID> userDTOToId = ctx -> {
        if (ctx.getSource() == null)
            return null;
        return ctx.getSource().getId();
    };

    private final Converter<UserCroppedDTO, UUID> userCroppedDTOToId = ctx -> {
        if (ctx.getSource() == null)
            return null;
        return ctx.getSource().getId();
    };

    private final Converter<List<ContractCroppedDTO>, List<UUID>> contractCroppedDTOToId = ctx -> {
        if (ctx.getSource() == null)
            return null;
        List<UUID> ids = new ArrayList<>();
        for (ContractCroppedDTO contractCroppedDTO : ctx.getSource()) {
            ids.add(contractCroppedDTO.getId());
        }
        return ids;
    };

    private final Converter<List<TeamCroppedDTO>, List<UUID>> teamCroppedDTOToId = ctx -> {
        if (ctx.getSource() == null)
            return null;
        List<UUID> ids = new ArrayList<>();
        for (TeamCroppedDTO teamCroppedDTO : ctx.getSource()) {
            ids.add(teamCroppedDTO.getId());
        }
        return ids;
    };
    private final Converter<List<Contract>, List<UUID>> contractToId = ctx -> {
        if (ctx.getSource() == null)
            return null;
        List<UUID> ids = new ArrayList<>();
        for (Contract contract : ctx.getSource()) {
            ids.add(contract.getId());
        }
        return ids;
    };

    private final Converter<UserRepresentation, KeycloakUserDTO> representationToKeycloak = ctx -> {
        if (ctx.getSource() == null) {
            return null;
        }
        var representation = ctx.getSource();

        return KeycloakUserDTO.builder()
                .ldapId(getLdapId(representation))
                .keycloakId(UUID.fromString(representation.getId()))
                .email(representation.getEmail())
                .login(representation.getUsername())
                .firstName(representation.getFirstName())
                .lastName(representation.getLastName())
                .photo(getUserPhoto(representation))
                .personalNumber(getEmployeeId(representation))
                .userRoles(null).build();
    };

    private String getUserPhoto(UserRepresentation user) {
        var profilePhoto = user.getAttributes().get("photo");
        if (profilePhoto == null || profilePhoto.size() < 1) return null;

        return profilePhoto.get(0);
    }

    private String getEmployeeId(UserRepresentation user) {
        var employeeId = user.getAttributes().get("employeeId");
        if (employeeId == null || employeeId.isEmpty()) return "-1";
        return employeeId.get(0);
    }

    private UUID getLdapId(UserRepresentation user) {
        var ldapId = user.getAttributes().get("LDAP_ID");
        if (ldapId == null || ldapId.isEmpty()) return null;
        return UUID.fromString(ldapId.get(0));
    }

    @Bean
    public void UserConverting() {
        mapper.getConfiguration().setAmbiguityIgnored(true);
        mapper.typeMap(User.class, UUID.class).setConverter(userToId);
        mapper.typeMap(UUID.class, User.class).setConverter(idToUser);

        mapper.typeMap(UserDTO.class, UUID.class).setConverter(userDTOToId);
        mapper.typeMap(UserCroppedDTO.class, UUID.class).setConverter(userCroppedDTOToId);

        mapper.typeMap(User.class, UserCroppedDTO.class).addMappings(em -> {
            em.using(contractToId).map(User::getOwnedContracts, UserCroppedDTO::setOwnedContractIds);
            em.map(User::getTeams, UserCroppedDTO::setTeamsIds);
            em.map(User::getOwnedTeams, UserCroppedDTO::setOwnedTeamsIds);
        });

        mapper.typeMap(UserDTO.class, UserCroppedDTO.class)
                .addMappings(em -> {
                            em.using(contractCroppedDTOToId).map(UserDTO::getOwnedContracts, UserCroppedDTO::setOwnedContractIds);
                            em.using(teamCroppedDTOToId).map(UserDTO::getTeams, UserCroppedDTO::setTeamsIds);
                            em.using(teamCroppedDTOToId).map(UserDTO::getOwnedTeams, UserCroppedDTO::setOwnedTeamsIds);
                        }
                );

        mapper.typeMap(UserCreateDTO.class, User.class).addMappings(em -> {
            em.skip(User::setOwnedContracts);
            em.skip(User::setTeams);
            em.using(fullName).map(u -> u, User::setFullName);
        });

        mapper.typeMap(User.class, UserDTO.class).addMappings(em -> {
            em.map(User::getOwnedContracts, UserDTO::setOwnedContracts);
            em.map(User::getTeams, UserDTO::setTeams);
            em.map(User::getOwnedTeams, UserDTO::setOwnedTeams);
        });

        mapper.typeMap(UserEditDTO.class, UserCreateDTO.class).addMappings(
                em -> {
                    em.skip(UserCreateDTO::setRemoved);
                    em.map(UserEditDTO::getId, UserCreateDTO::setId);
                    em.map(UserEditDTO::getId, UserCreateDTO::setId);
                    em.map(UserEditDTO::getUserRoles, UserCreateDTO::setUserRoles);
                }
        );

        mapper.typeMap(UserEditDTO.class, KeycloakUserDTO.class).addMappings(
                em -> em.skip(KeycloakUserDTO::setKeycloakId)
        );

        mapper.typeMap(LdapUser.class, UserEditDTO.class)
                .addMappings(em -> {
                    em.map(LdapUser::getObjectGUID, UserEditDTO::setId);
                    em.map(LdapUser::getUserPrincipalName, UserEditDTO::setLogin);
                    em.skip(UserEditDTO::setUserRoles);
                });

        mapper.typeMap(UserEditDTO.class, User.class).addMappings(
                em -> {
                    em.skip(User::setRemoved);
                    em.skip(User::setAuditInfo);
                    em.skip(User::setFullName);
                    em.skip(User::setOwnedContracts);
                    em.skip(User::setTeams);
                }
        );

        mapper.typeMap(UserRepresentation.class, KeycloakUserDTO.class).setConverter(representationToKeycloak);

        mapper.typeMap(KeycloakUserDTO.class, UserCreateDTO.class).addMappings(
                em -> {
                    em.map(KeycloakUserDTO::getLdapId, UserCreateDTO::setId);
                    em.skip(UserCreateDTO::setAuditInfo);
                    em.skip(UserCreateDTO::setRemoved);
                }
        );

        mapper.typeMap(KeycloakUserDTO.class, UserEditDTO.class).addMappings(em ->
        {
            em.map(KeycloakUserDTO::getLdapId, UserEditDTO::setId);
            em.skip(UserEditDTO::setAuditInfo);
        });
    }
}
