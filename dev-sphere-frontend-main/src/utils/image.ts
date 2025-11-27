// src/utils/image.ts

/**
 * Formats image URLs to handle Mixed Content issues by using a local proxy for insecure origins.
 * @param url The original image URL
 * @returns The formatted URL (relative proxy path if needed)
 */
export const formatImageUrl = (url: string | undefined | null) => {
    if (!url) return ''

    // If URL points to the insecure image server, rewrite to use local proxy
    // Matches http://10.104.0.111:9000/poap and rewrites to /poap
    if (url.includes('10.104.0.111:9000/poap')) {
        return url.replace('http://10.104.0.111:9000/poap', '/poap')
    }

    return url
}
