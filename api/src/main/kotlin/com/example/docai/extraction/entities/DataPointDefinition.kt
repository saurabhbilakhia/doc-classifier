package com.example.docai.extraction.entities

import com.example.docai.classification.entities.Classification
import com.example.docai.common.enums.DataType
import com.example.docai.common.enums.RuleType
import jakarta.persistence.*

@Entity
@Table(
    name = "data_point_definitions",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_dp_def_per_class_key", columnNames = ["classification_id", "key"])
    ]
)
class DataPointDefinition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    var classification: Classification,

    @Column(nullable = false, length = 100)
    var key: String,

    @Column(length = 200)
    var label: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: DataType = DataType.STRING,

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    var ruleType: RuleType = RuleType.REGEX,

    @Column(columnDefinition = "text", nullable = false)
    var expression: String,

    @Column(nullable = false)
    var required: Boolean = false
)
