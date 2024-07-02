package run.halo.search.algolia;

import com.algolia.search.DefaultSearchClient;
import com.algolia.search.SearchClient;
import com.algolia.search.SearchConfig;
import com.algolia.search.SearchIndex;
import com.algolia.search.models.indexing.Query;
import com.algolia.search.models.settings.IndexSettings;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Secret;
import run.halo.app.search.HaloDocument;
import run.halo.app.search.SearchEngine;
import run.halo.app.search.SearchOption;
import run.halo.app.search.SearchResult;

@Slf4j
@Component
public class AlgoliaSearchEngine implements SearchEngine, DisposableBean,
    ApplicationListener<ConfigUpdatedEvent> {

    private final ExtensionClient client;

    private SearchClient searchClient;

    private SearchIndex<AlgoliaDocument> index;

    private volatile boolean available = false;

    public AlgoliaSearchEngine(ExtensionClient client) {
        this.client = client;
    }

    private void refresh(String applicationId, String apiKey, String indexName) {
        if (this.available) {
            try {
                this.destroy();
            } catch (Exception e) {
                // ignore it
                log.warn("Failed to destroy AlgoliaSearchEngine during refreshing config", e);
            }
        }
        this.searchClient =
            DefaultSearchClient.create(new SearchConfig.Builder(applicationId, apiKey).build());
        this.index = this.searchClient.initIndex(indexName, AlgoliaDocument.class);
        this.index.setSettings(new IndexSettings()
            .setAttributesToRetrieve(List.of("id", "metadataName", "title",
                "annotations", "description", "categories", "tags", "published", "recycled",
                "exposed", "ownerName", "creationTimestamp", "updateTimestamp", "permalink",
                "type"))
            .setSearchableAttributes(List.of("title", "description", "content")));
        this.available = true;
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public void addOrUpdate(Iterable<HaloDocument> docs) {
        var docStream = Streams.of(docs).map(doc -> {
            var algoliaDoc = new AlgoliaDocument();
            algoliaDoc.setObjectID(doc.getId());
            algoliaDoc.setDocument(doc);
            return algoliaDoc;
        });
        this.index.saveObjects(docStream::iterator);
    }

    @Override
    public void deleteDocument(Iterable<String> docIds) {
        this.index.deleteObjects(Streams.of(docIds).toList());
    }

    @Override
    public void deleteAll() {
        this.index.clearObjects();
    }

    @Override
    public SearchResult search(SearchOption searchOption) {
        var algoliaSearchResult = this.index.search(
            new Query(searchOption.getKeyword())
                .setHitsPerPage(searchOption.getLimit())
                .setHighlightPreTag(searchOption.getHighlightPreTag())
                .setHighlightPostTag(searchOption.getHighlightPostTag())
                .setAttributesToSnippet(List.of("title:50", "description:100", "content:200"))
            // TODO Set filters
            // .setFilters("")
        );

        var searchResult = new SearchResult();
        searchResult.setLimit(searchOption.getLimit());
        searchResult.setTotal(algoliaSearchResult.getNbHits());
        searchResult.setKeyword(searchOption.getKeyword());
        searchResult.setProcessingTimeMillis(algoliaSearchResult.getProcessingTimeMS());
        searchResult.setHits(algoliaSearchResult.getHits()
            .stream()
            .map(algoliaDoc -> {
                System.out.println("DEBUG: " + algoliaDoc);
                var document = algoliaDoc.getDocument();
                algoliaDoc.getSnippetResult().forEach((attr, hr) -> {
                    var value = hr.getValue();
                    if (value != null) {
                        switch (attr) {
                            case "title":
                                document.setTitle(value);
                                break;
                            case "description":
                                document.setDescription(value);
                                break;
                            case "content":
                                document.setContent(value);
                                break;
                        }
                    }
                });
                return document;
            })
            .toList());
        return searchResult;
    }

    @Override
    public void destroy() throws Exception {
        if (this.searchClient != null) {
            this.searchClient.close();
        }
    }

    @Override
    public void onApplicationEvent(ConfigUpdatedEvent event) {
        log.info("Algolia configuration changed");
        var properties = event.getAlgoliaProperties();
        var secretName = properties.getSecretName();
        var secretOpt = client.fetch(Secret.class, secretName);
        if (secretOpt.isEmpty()) {
            return;
        }
        var secret = secretOpt.get();
        var stringData = secret.getStringData();
        var apiKeyKey = "apiKey";
        var appIdKey = "appId";
        if (stringData == null
            || !(stringData.containsKey(apiKeyKey) && stringData.containsKey(appIdKey))) {
            return;
        }
        log.info("Refreshing Algolia configuration");
        var apiKey = stringData.get(apiKeyKey);
        var appId = stringData.get(appIdKey);
        this.refresh(appId, apiKey, properties.getIndexName());
    }

    @Data
    static class AlgoliaDocument {

        private String objectID;

        @JsonProperty(value = "_highlightResult", access = JsonProperty.Access.WRITE_ONLY)
        private Map<String, HighlightResult> highlightResult;

        @JsonProperty(value = "_snippetResult", access = JsonProperty.Access.WRITE_ONLY)
        private Map<String, SnippetResult> snippetResult;

        @JsonUnwrapped
        private HaloDocument document;

    }

    @Data
    static class HighlightResult {

        private String value;

        private String matchLevel;

        private List<String> matchWords;

        private boolean fullyHighlighted;

    }

    @Data
    static class SnippetResult {

        private String value;

        private String matchLevel;

    }

}
