package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.mapper.AccountMapper;
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
  AccountMapper accountMapper;

  @Transactional(readOnly = true)
  public List<AccountResponse> getAccounts() {
    return accountRepository.findAll().stream().map(accountMapper::toAccountResponse).toList();
  }
}
