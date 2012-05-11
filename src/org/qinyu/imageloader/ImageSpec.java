package org.qinyu.imageloader;


/**
 * The Class ImageSpec.
 */
public class ImageSpec {

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ThumbSpec [width=" + width + ", height=" + height + ", orientation=" + orientation + ", url=" + url
                + "]";
    }

    /** The width. */
    protected int width;

    /** The height. */
    protected int height;

    /** The orientation. */
    protected int orientation;

    /** The url. */
    protected final String url;

    /**
     * Instantiates a new image spec.
     * 
     * @param url
     *            the url
     */
    public ImageSpec(String url) {
        this.url = url;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageSpec other = (ImageSpec) obj;
        if (height != other.height)
            return false;
        if (orientation != other.orientation)
            return false;
        if (getUniqueId() == null) {
            if (other.getUniqueId() != null)
                return false;
        } else if (!getUniqueId().equals(other.getUniqueId()))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    /**
     * Gets the cache file name.
     * 
     * @return the cache file name
     */
    public String getCacheFileName() {
        return Integer.toHexString(getUniqueId().hashCode());
    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    String getUrl() {
        return url;
    }

    /**
     * Gets the unique id.
     * 
     * @return the unique id
     */
    protected String getUniqueId() {
        return url;
    }

}
