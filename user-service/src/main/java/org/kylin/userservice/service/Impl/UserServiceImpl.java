package org.kylin.userservice.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.kylin.userservice.constant.InfoConstant;
import org.kylin.userservice.dto.AuthDto;
import org.kylin.userservice.dto.Filter;
import org.kylin.userservice.dto.UserDto;
import org.kylin.userservice.entity.User;
import org.kylin.userservice.exception.UserOperationException;
import org.kylin.userservice.repository.CommonRepository;
import org.kylin.userservice.repository.UserRepository;
import org.kylin.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private CommonRepository<User> commonRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, CommonRepository<User> commonRepository) {
        this.userRepository = userRepository;
        this.commonRepository = commonRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    @Override
    public User save(UserDto userDto) {
        checkUserCreateInfo(userDto);

        User user = User.builder()
                .username(userDto.getUsername())
                .age(userDto.getAge())
                .phoneNumber(userDto.getPhoneNumber())
                .email(userDto.getEmail())
                .build();

        User result;
        try {
            result = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            Throwable exception = Optional.ofNullable(e.getRootCause()).orElse(e);
            if (exception.getMessage().contains(InfoConstant.DUPLICATE)) {
                log.error(InfoConstant.USER_HAS_ALREADY_EXIST);
                throw new UserOperationException(InfoConstant.USER_HAS_ALREADY_EXIST);
            } else {
                log.error(exception.getMessage());
                throw new UserOperationException(exception.getMessage());
            }
        }

        log.info("Send authorization message to auth-center...");
        createDefaultAuthUser(AuthDto.builder().id(result.getId()).username(userDto.getUsername())
                .password(userDto.getPassword()).build());
        return result;
    }

    @Override
    public List<User> queryByFilter(List<Filter> filters) {
        return commonRepository.getEntitiesByFilter(User.class, filters);
    }

    private void checkUserCreateInfo(UserDto userDto) {
        List<String> infos = new ArrayList<>();

        if (null == userDto.getUsername() || "".equals(userDto.getUsername())) {
            infos.add(MessageFormat.format(InfoConstant.PROPERTIES_CANNOT_BE_EMPTY_1, InfoConstant.USERNAME));
        }

        if (null == userDto.getPassword()) {
            infos.add(MessageFormat.format(InfoConstant.PROPERTIES_CANNOT_BE_EMPTY_1, InfoConstant.PASSWORD));
        } else if (userDto.getPassword().length() < 6) {
            infos.add(MessageFormat.format(InfoConstant.PASSWORD_LEAST_CHAR_1, 6));
        }

        if (!infos.isEmpty()) {
            log.error(infos.toString());
            throw new UserOperationException(infos.toString());
        }
    }

    private void createDefaultAuthUser(AuthDto dto) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthDto> httpEntity = new HttpEntity<>(dto, headers);
        restTemplate.exchange("http://auth-center:18080/users/auth", HttpMethod.POST, httpEntity, Void.class);
        //restTemplate.exchange("http://localhost:18080/users/auth", HttpMethod.POST, httpEntity, Void.class);

    }
}
