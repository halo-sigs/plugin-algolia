package run.halo.search.algolia;

import lombok.Data;

@Data
public class AlgoliaProperties {

    /**
     * Secret name.
     */
    private String secretName;

    /**
     * Index name.
     */
    private String indexName = "halo";

}
