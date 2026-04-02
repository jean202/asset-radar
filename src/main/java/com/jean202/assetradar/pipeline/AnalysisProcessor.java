package com.jean202.assetradar.pipeline;

import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AnalysisProcessor {
    public Flux<AssetPrice> passThrough(Flux<AssetPrice> source) {
        return source.filter(this::isValid)
                .distinctUntilChanged(price -> "%s:%s:%s:%s:%s".formatted(
                        price.source(),
                        price.quoteCurrency(),
                        price.symbol(),
                        price.price(),
                        price.signedChangeRate()
                ));
    }

    private boolean isValid(AssetPrice price) {
        return price != null
                && price.price() != null
                && price.price().compareTo(BigDecimal.ZERO) > 0
                && price.collectedAt() != null;
    }
}
