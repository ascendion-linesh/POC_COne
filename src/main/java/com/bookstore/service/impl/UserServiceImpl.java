package com.bookstore.service.impl;

import com.bookstore.domain.ShoppingCart;
import com.bookstore.domain.User;
import com.bookstore.domain.UserPayment;
import com.bookstore.domain.UserShipping;
import com.bookstore.domain.security.PasswordResetToken;
import com.bookstore.domain.security.UserRole;
import com.bookstore.repository.PasswordResetTokenRepository;
import com.bookstore.repository.RoleRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public PasswordResetToken getPasswordResetToken(final String token) {
        return passwordResetTokenRepository.findByToken(token).orElse(null);
    }

    @Override
    public void createPasswordResetTokenForUser(final User user, final String token) {
        final PasswordResetToken myToken = passwordResetTokenRepository.findByUser(user).orElse(null);
        if (myToken != null) {
            myToken.updateToken(token);
            passwordResetTokenRepository.save(myToken);
        } else {
            final PasswordResetToken newToken = new PasswordResetToken(token, user);
            passwordResetTokenRepository.save(newToken);
        }
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional
    public User createUser(User user, Set<UserRole> userRoles) throws Exception {
        User localUser = userRepository.findByUsername(user.getUsername()).orElse(null);

        if (localUser != null) {
            LOG.info("User with username {} already exists.", user.getUsername());
            throw new Exception("User already exists");
        } else {
            for (UserRole ur : userRoles) {
                roleRepository.save(ur.getRole());
            }
            user.getUserRoles().addAll(userRoles);
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUser(user);
            user.setShoppingCart(shoppingCart);
            localUser = userRepository.save(user);
        }
        return localUser;
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public void setUserDefaultPayment(Long userPaymentId, User user) {
        for (UserPayment userPayment : user.getUserPaymentList()) {
            userPayment.setDefaultPayment(userPayment.getId().equals(userPaymentId));
        }
        userRepository.save(user);
    }

    @Override
    public void setUserDefaultShipping(Long userShippingId, User user) {
        for (UserShipping userShipping : user.getUserShippingList()) {
            userShipping.setUserShippingDefault(userShipping.getId().equals(userShippingId));
        }
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}