package com.example.clients.transfer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "transfer",
        url = "${clients.transfer.url}"
)
public interface TransferClient {

    @PostMapping(value = "/user/{login}/transfer",
            consumes = "application/json",
            produces = "application/json")
    TransferResponse transfer(@PathVariable("login") String login,
                              @RequestBody TransferRequest rq);
}