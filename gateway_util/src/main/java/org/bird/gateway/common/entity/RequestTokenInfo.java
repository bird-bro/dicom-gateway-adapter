package org.bird.gateway.common.entity;

/**
 * @author bird
 * @date 2021-7-1 16:37
 **/
public class RequestTokenInfo {
    private String accessKey;
    private String timestamp;
    private String sign;


    public String getAccessKey() {
        return this.accessKey;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getSign() {
        return this.sign;
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey = accessKey;
    }

    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    public void setSign(final String sign) {
        this.sign = sign;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RequestTokenInfo)) {
            return false;
        } else {
            RequestTokenInfo other = (RequestTokenInfo)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                label47: {
                    Object this$accessKey = this.getAccessKey();
                    Object other$accessKey = other.getAccessKey();
                    if (this$accessKey == null) {
                        if (other$accessKey == null) {
                            break label47;
                        }
                    } else if (this$accessKey.equals(other$accessKey)) {
                        break label47;
                    }

                    return false;
                }

                Object this$timestamp = this.getTimestamp();
                Object other$timestamp = other.getTimestamp();
                if (this$timestamp == null) {
                    if (other$timestamp != null) {
                        return false;
                    }
                } else if (!this$timestamp.equals(other$timestamp)) {
                    return false;
                }

                Object this$sign = this.getSign();
                Object other$sign = other.getSign();
                if (this$sign == null) {
                    if (other$sign != null) {
                        return false;
                    }
                } else if (!this$sign.equals(other$sign)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof RequestTokenInfo;
    }

    @Override
    public int hashCode() {
        boolean PRIME = true;
        int result = 1;
        Object $accessKey = this.getAccessKey();
        result = result * 59 + ($accessKey == null ? 43 : $accessKey.hashCode());
        Object $timestamp = this.getTimestamp();
        result = result * 59 + ($timestamp == null ? 43 : $timestamp.hashCode());
        Object $sign = this.getSign();
        result = result * 59 + ($sign == null ? 43 : $sign.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "RequestTokenInfo(accessKey=" + this.getAccessKey() + ", timestamp=" + this.getTimestamp() + ", sign=" + this.getSign() + ")";
    }
}
