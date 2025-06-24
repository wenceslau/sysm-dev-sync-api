package com.sysm.devsync.application;

import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.UserCreateUpdate;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import org.springframework.util.StringUtils;

public class UserService {

    private final UserPersistencePort userPersistence;

    public UserService(UserPersistencePort userPersistence) {
        this.userPersistence = userPersistence;
    }

    public CreateResponse createUser(UserCreateUpdate userCreateUpdate) {
        User user = User.create(
                userCreateUpdate.name(),
                userCreateUpdate.email(),
                userCreateUpdate.userRole()
        );
        userPersistence.create(user);
        return new CreateResponse(user.getId());
    }

    public void updateUser(String userId, UserCreateUpdate userUpdate) {
        User user = userPersistence.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found", userId));

        user.update(
                userUpdate.name(),
                userUpdate.email(),
                userUpdate.userRole()
        );

        if (StringUtils.hasText(userUpdate.profilePictureUrl())) {
            user.updateProfilePicture(userUpdate.profilePictureUrl());
        }

        userPersistence.update(user);
    }

    public void updateUserPatch(String userId, UserCreateUpdate userUpdate) {
        User user = userPersistence.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found", userId));

        if (StringUtils.hasText(userUpdate.name())) {
            user.updateName(userUpdate.name());
        }
        if (StringUtils.hasText(userUpdate.email())) {
            user.updateEmail(userUpdate.email());
        }
        if (userUpdate.userRole() != null) {
            user.updateUserRole(userUpdate.userRole());
        }
        if (StringUtils.hasText(userUpdate.profilePictureUrl())) {
            user.updateProfilePicture(userUpdate.profilePictureUrl());
        }

        userPersistence.update(user);
    }

    public void deleteUser(String userId) {
        if (!userPersistence.existsById(userId)) {
            throw new NotFoundException("User not found", userId);
        }
        userPersistence.deleteById(userId);
    }

    public User getUserById(String userId) {
        return userPersistence.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found", userId));
    }

    public Pagination<User> searchUsers(SearchQuery query) {
        return userPersistence.findAll(query);
    }

}
