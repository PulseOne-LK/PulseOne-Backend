# Video Consultation Service - AWS Chime Setup Guide

## Overview

This guide will help you set up AWS Chime SDK for video consultations while staying within the Free Tier limits.

## AWS Free Tier Limits

AWS Chime SDK offers:

- **1,000 attendee-minutes per month** (Free Tier)
- **Calculation**: Each participant × minutes in meeting
- **Example**: 30-minute call with 2 people = 60 attendee-minutes

### Monitoring Usage

To stay within limits:

1. Check `/api/video/metrics/usage` endpoint regularly
2. Monitor AWS CloudWatch metrics
3. Set up billing alerts in AWS Console

## Step-by-Step AWS Setup

### 1. Create AWS Account

1. Go to https://aws.amazon.com
2. Click "Create an AWS Account"
3. Follow the registration process
4. **Important**: Requires credit card but won't charge within Free Tier

### 2. Create IAM User for Chime

1. Login to AWS Console
2. Go to **IAM** (Identity and Access Management)
3. Click **Users** → **Add User**
4. Enter username: `pulseone-video-service`
5. Select **Programmatic access**
6. Click **Next: Permissions**

### 3. Attach Required Permissions

**Option A: Using Managed Policy (Recommended for Development)**

1. Click **Attach existing policies directly**
2. Search for `AmazonChimeSDK`
3. Select **AmazonChimeSDK** or **AmazonChimeSDKMediaPipelines**

**Option B: Custom Policy (Recommended for Production)**

1. Click **Create policy** → **JSON**
2. Paste this policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ChimeMeetingOperations",
      "Effect": "Allow",
      "Action": [
        "chime:CreateMeeting",
        "chime:CreateMeetingWithAttendees",
        "chime:DeleteMeeting",
        "chime:GetMeeting",
        "chime:ListMeetings",
        "chime:CreateAttendee",
        "chime:BatchCreateAttendee",
        "chime:DeleteAttendee",
        "chime:GetAttendee",
        "chime:ListAttendees",
        "chime:StartMeetingTranscription",
        "chime:StopMeetingTranscription",
        "chime:TagResource",
        "chime:UntagResource",
        "chime:ListTagsForResource"
      ],
      "Resource": "*"
    },
    {
      "Sid": "CloudWatchMetrics",
      "Effect": "Allow",
      "Action": ["cloudwatch:PutMetricData"],
      "Resource": "*"
    }
  ]
}
```

3. Click **Review policy**
4. Name: `PulseOneChimeVideoPolicy`
5. Click **Create policy**
6. Go back to user creation and attach this policy

### 4. Get Access Keys

1. Complete user creation
2. **Download the CSV** with credentials
3. Save securely - you won't see the secret key again!

The CSV contains:

- Access Key ID
- Secret Access Key

### 5. Configure Service

Update `.env` file:

```env
AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY_FROM_STEP_4
AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY_FROM_STEP_4
AWS_REGION=us-east-1
```

### 6. Choose AWS Region

Recommended regions based on location:

| Location      | AWS Region  | Code           |
| ------------- | ----------- | -------------- |
| US East Coast | N. Virginia | us-east-1      |
| US West Coast | Oregon      | us-west-2      |
| Europe        | Ireland     | eu-west-1      |
| Asia Pacific  | Singapore   | ap-southeast-1 |

**Tip**: Choose the region closest to your users for best performance.

### 7. Test Configuration

Run the service:

```bash
python main.py
```

Check health:

```bash
curl http://localhost:8086/health
```

Should show:

```json
{
  "aws_chime": "configured"
}
```

## Security Best Practices

### 1. Never Commit Credentials

Add to `.gitignore`:

```
.env
*.pem
*.key
```

### 2. Use Environment Variables

```bash
# Windows
set AWS_ACCESS_KEY_ID=your_key
set AWS_SECRET_ACCESS_KEY=your_secret

# Linux/Mac
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

### 3. Rotate Access Keys Regularly

1. Create new access key
2. Update application
3. Test thoroughly
4. Delete old key

### 4. Enable MFA on AWS Account

1. Go to IAM → Your Security Credentials
2. Enable Multi-Factor Authentication
3. Use authenticator app

### 5. Set Up Billing Alerts

1. Go to **AWS Billing Console**
2. Click **Billing preferences**
3. Enable **Receive Free Tier Usage Alerts**
4. Enter your email
5. Set threshold: $1 (to catch any overages)

## Cost Monitoring

### CloudWatch Dashboard

1. Go to **CloudWatch** in AWS Console
2. Create dashboard: `PulseOne-Video-Usage`
3. Add metrics:
   - `AWS/Chime` → `MeetingDuration`
   - `AWS/Chime` → `AttendeeCount`

### Usage Tracking in Application

The service automatically tracks:

- Total attendee-minutes per month
- Number of sessions
- Active sessions

Check usage:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8086/api/video/metrics/usage
```

Response:

```json
{
  "total_attendee_minutes_current_month": 450,
  "free_tier_limit": 1000,
  "remaining_minutes": 550,
  "percentage_used": 45.0
}
```

## Troubleshooting

### Error: "Unable to locate credentials"

**Solution**:

1. Check `.env` file exists and has AWS keys
2. Ensure no extra spaces in keys
3. Restart the service

### Error: "Access Denied"

**Solution**:

1. Verify IAM user has correct permissions
2. Check if policy is attached to user
3. Try creating a new access key

### Error: "Region not available"

**Solution**:

1. Chime SDK is available in specific regions
2. Use one of: us-east-1, us-west-2, eu-west-1, eu-central-1, ap-southeast-1
3. Update `AWS_REGION` in `.env`

### High Usage Warning

If usage exceeds 80%:

1. Review session durations
2. Check for stuck sessions
3. Implement session time limits
4. Consider upgrading to paid tier

## AWS Free Tier Expiration

After 12 months:

- Chime SDK charges apply
- **Cost**: ~$0.0017 per attendee-minute
- **Example**: 1000 attendee-minutes = $1.70/month

Plan ahead:

1. Monitor usage trends
2. Estimate post-free-tier costs
3. Optimize session durations
4. Consider peak usage times

## Additional Resources

- [AWS Chime SDK Documentation](https://docs.aws.amazon.com/chime-sdk/)
- [Pricing Calculator](https://calculator.aws/)
- [Free Tier FAQ](https://aws.amazon.com/free/free-tier-faqs/)
- [Chime SDK GitHub](https://github.com/aws/amazon-chime-sdk-js)

## Support

If you encounter issues:

1. Check AWS Service Health Dashboard
2. Review CloudWatch Logs
3. Check application logs: `main.py` output
4. Verify database connectivity
5. Test RabbitMQ connection

## Production Deployment

For production:

1. Use AWS Secrets Manager for credentials
2. Set up CloudWatch alarms
3. Enable AWS CloudTrail for auditing
4. Use VPC endpoints for security
5. Implement rate limiting
6. Add session recording (costs extra)

## Next Steps

After AWS setup:

1. ✅ Test video session creation
2. ✅ Verify meeting join flow
3. ✅ Check event publishing
4. ✅ Monitor usage metrics
5. ✅ Set up billing alerts
