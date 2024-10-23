package com.devglan.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity // Marks this class as a JPA entity
public class BlogPost {
    
    @Id // Specifies the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatically generates values for the primary key
    private Long id; // Primary key field

    private String title;
    @Lob
    private String description;
    private String link;
    @Lob
    private String imageUrl;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

	public String getImgUrl() {
		return imageUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imageUrl = imgUrl;
	}
}
