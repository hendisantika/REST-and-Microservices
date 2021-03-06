package com.edufect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.edufect.exception.AccountNotFoundException;
import com.edufect.exception.ForbiddenException;
import com.edufect.model.Accounts;
import com.edufect.model.NetBankings;
import com.edufect.model.Transactions;
import com.edufect.repository.NetBankingsRepository;

@RestController
@CrossOrigin(allowedHeaders = "*", value = "*")
public class BankAccountsController {

	private static final Logger LOG = LoggerFactory.getLogger(BankAccountsController.class);

	@Autowired
	private NetBankingsRepository netBankingsRepository;

	@GetMapping(value = "/netbankings", produces = "application/json")
	public Page<NetBankings> getNetbankings(Pageable pageable) {
		LOG.info("GET");
		return netBankingsRepository.findAll(pageable);
	}

	@PostMapping(value = "/netbankings/{accId}", produces = "application/json")
	public ResponseEntity<NetBankings> postNetBanking(@RequestBody NetBankings newNetBankings,
			@PathVariable long accId) {
		LOG.info("POST");
		RestTemplate restTemplate = new RestTemplate();

		String urlToGetAccount = "http://localhost:2221/accounts/" + accId;
		Accounts account = restTemplate.getForObject(urlToGetAccount, Accounts.class);
		if (account != null) {

			String urlToGetBalance = "http://localhost:2221/accounts/balance/" + accId;
			long currentBalance = restTemplate.getForObject(urlToGetBalance, Long.class);

			if ((Math.abs(newNetBankings.getAmount()) <= currentBalance || newNetBankings.getAmount() > 0)) {
				newNetBankings.setAccId(accId);
				NetBankings netBankings = netBankingsRepository.save(newNetBankings);
				Transactions transactions = new Transactions();
				transactions.setAccounts(account);
				transactions.setAmount(netBankings.getAmount());
				transactions.setPartyName(netBankings.getPartyName());
				transactions.setType("By NetBanking");
				transactions.setTypeCode(netBankings.getLoginName());
				transactions.setTypeId(netBankings.getNetTransId());
				String urlToPostTransaction = "http://localhost:2221/accounts/transactions/" + accId;
				Transactions transactionResult = restTemplate.postForObject(urlToPostTransaction, transactions,
						Transactions.class);
				LOG.debug("Transaction:{}", transactions);
				LOG.debug("Transaction Result:{}", transactionResult);
				LOG.debug("NetBanking:{}", netBankings);
				return new ResponseEntity<>(netBankings, HttpStatus.OK);
			} else {

				throw new ForbiddenException();
			}
		} else {
			throw new AccountNotFoundException();
		}

	}

	@GetMapping(value = "/netbankings/{accId}", produces = "application/json")
	public Page<NetBankings> getNetbankingByAccId(@PathVariable long accId, Pageable pageable) {
		LOG.info("GET:{} ", accId);
		RestTemplate restTemplate = new RestTemplate();
		String urlToGetAccount = "http://localhost:2221/accounts/" + accId;
		Accounts account = restTemplate.getForObject(urlToGetAccount, Accounts.class);
		if (account != null) {
			Page<NetBankings> netBankingList = netBankingsRepository.findByAccId(accId, pageable);
			LOG.debug("NetBanking List:{}", netBankingList);
			return netBankingList;
		} else {
			throw new AccountNotFoundException();
		}
	}

}
