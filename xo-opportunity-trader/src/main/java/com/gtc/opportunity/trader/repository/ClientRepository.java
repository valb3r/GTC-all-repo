package com.gtc.opportunity.trader.repository;

import com.gtc.opportunity.trader.domain.Client;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by Valentyn Berezin on 06.03.18.
 */
public interface ClientRepository extends CrudRepository<Client, String> {

    List<Client> findByEnabledTrue();
}
