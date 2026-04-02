package com.jean202.assetradar.config;

import com.jean202.assetradar.alert.AlertSeverity;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.alert.notifier")
public class AlertNotifierProperties {
    private AsyncProperties async = new AsyncProperties();
    private SlackProperties slack = new SlackProperties();
    private WebhookProperties webhook = new WebhookProperties();

    public AsyncProperties getAsync() {
        return async;
    }

    public void setAsync(AsyncProperties async) {
        this.async = async;
    }

    public SlackProperties getSlack() {
        return slack;
    }

    public void setSlack(SlackProperties slack) {
        this.slack = slack;
    }

    public WebhookProperties getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookProperties webhook) {
        this.webhook = webhook;
    }

    public static class AsyncProperties {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 200;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class ChannelProperties {
        private boolean enabled;
        private AlertSeverity minimumSeverity = AlertSeverity.CRITICAL;
        private Duration timeout = Duration.ofSeconds(3);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public AlertSeverity getMinimumSeverity() {
            return minimumSeverity;
        }

        public void setMinimumSeverity(AlertSeverity minimumSeverity) {
            this.minimumSeverity = minimumSeverity;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class SlackProperties extends ChannelProperties {
        private String webhookUrl = "";

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }

    public static class WebhookProperties extends ChannelProperties {
        private String url = "";
        private String authToken = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }
    }
}
