package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.repository.AccountRepository;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

  AccountRepository accountRepository;

  @Transactional(readOnly = true)
  public List<AccountResponse> getAccounts() {
    return accountRepository.findAll().stream().map(this::toResponse).toList();
  }

  private AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getEmail(),
        account.getRole(),
        account.isEnabled(),
        account.getCreatedAt());
  }
}
