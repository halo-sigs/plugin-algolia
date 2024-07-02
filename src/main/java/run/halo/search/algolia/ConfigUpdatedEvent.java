package run.halo.search.algolia;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConfigUpdatedEvent extends ApplicationEvent {

    private final AlgoliaProperties algoliaProperties;

    public ConfigUpdatedEvent(Object source, AlgoliaProperties algoliaProperties) {
        super(source);
        this.algoliaProperties = algoliaProperties;
    }

}
