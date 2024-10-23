package com.devglan.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devglan.model.BlogPost;
import com.devglan.repository.BlogPostRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class RssFeedService {

    @Autowired
    private BlogPostRepository blogPostRepository;

    public List<BlogPost> fetchBlogPosts(String feedUrl) {
        List<BlogPost> blogPosts = new ArrayList<>();
        try {
            URL url = new URL(feedUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));

            for (SyndEntry entry : feed.getEntries()) {
                Optional<BlogPost> existingPost = blogPostRepository.findByLink(entry.getLink());

                if (!existingPost.isPresent()) {
                    BlogPost post = new BlogPost();
                    post.setTitle(entry.getTitle());
                    post.setLink(entry.getLink());

                    // Extract and clean up HTML content
                    String content = entry.getContents() != null && !entry.getContents().isEmpty()
                            ? entry.getContents().get(0).getValue()
                            : (entry.getDescription() != null ? entry.getDescription().getValue() : "No Content");

                    // Parse the HTML using JSoup to extract the first image URL
                    Document document = Jsoup.parse(content);
                    Element firstImage = document.select("img").first();
                    String imageUrl = firstImage != null ? firstImage.attr("src") : null;
                    post.setImgUrl(imageUrl);

                    // Clean HTML content using Jsoup
                    String cleanedContent = Jsoup.parse(content).text();
                    post.setDescription(cleanedContent);

                    // Save the new blog post to the database
                    blogPostRepository.save(post);
                    blogPosts.add(post);
                } else {
                    // If the post already exists, add it to the list as well
                    blogPosts.add(existingPost.get());
                    System.out.println("Duplicate post detected for link: " + entry.getLink());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blogPosts;
    }

    public List<BlogPost> getAllBlogPosts() {
        return blogPostRepository.findAll();
    }
}
