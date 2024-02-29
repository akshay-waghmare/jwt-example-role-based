package com.devglan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.devglan.dao.BetRepository;
import com.devglan.model.Bets;

import java.util.List;

@Service
public class BetService {

    @Autowired
    private BetRepository betRepository;

    public Bets getBetById(Long id) throws NotFoundException {
        return betRepository.findById(id)
                            .orElseThrow(() -> new NotFoundException());
    }

    public List<Bets> getBetsByUserId(Long userId) {
        return betRepository.findByUserId(userId);
    }

    // Other service methods (e.g., saveBet, updateBet, deleteBet) can be added here

}

