"""
AWS Chime SDK Integration Service
Handles all interactions with AWS Chime for video consultations
"""
import boto3
import logging
from typing import Dict, Optional, List
from datetime import datetime, timedelta
from app.config import settings
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)


class ChimeService:
    """
    Service for managing AWS Chime meetings and attendees
    Implements Free Tier optimizations
    """
    
    def __init__(self):
        """Initialize AWS Chime client"""
        self.chime_client = boto3.client(
            'chime-sdk-meetings',
            region_name=settings.AWS_REGION,
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY
        )
        logger.info(f"ChimeService initialized with region: {settings.AWS_REGION}")
    
    async def create_meeting(
        self,
        external_meeting_id: str,
        session_id: str,
        doctor_id: str,
        patient_id: str,
        media_region: Optional[str] = None
    ) -> Dict:
        """
        Create a new AWS Chime meeting
        
        Args:
            external_meeting_id: Custom meeting identifier
            session_id: Video consultation session ID
            doctor_id: Doctor's user ID
            patient_id: Patient's user ID
            media_region: AWS region for media routing (defaults to settings.AWS_REGION)
        
        Returns:
            Dict containing meeting details
        """
        try:
            # Use specified region or default
            region = media_region or settings.AWS_REGION
            
            # Create meeting with metadata
            response = self.chime_client.create_meeting(
                ClientRequestToken=session_id,  # Idempotency token
                ExternalMeetingId=external_meeting_id,
                MediaRegion=region,
                MeetingFeatures={
                    'Audio': {
                        'EchoReduction': 'AVAILABLE'  # Enable echo cancellation
                    },
                    'Video': {
                        'MaxResolution': 'HD'  # HD video quality
                    },
                    'Content': {
                        'MaxResolution': 'FHD'  # Full HD for screen sharing
                    }
                },
                Tags=[
                    {'Key': 'Service', 'Value': 'PulseOne'},
                    {'Key': 'Type', 'Value': 'VideoConsultation'},
                    {'Key': 'DoctorId', 'Value': doctor_id},
                    {'Key': 'PatientId', 'Value': patient_id},
                    {'Key': 'SessionId', 'Value': session_id}
                ]
            )
            
            meeting = response['Meeting']
            
            logger.info(f"Created Chime meeting: {meeting['MeetingId']} for session: {session_id}")
            
            return {
                'meeting_id': meeting['MeetingId'],
                'external_meeting_id': meeting['ExternalMeetingId'],
                'media_region': meeting['MediaRegion'],
                'media_placement': {
                    'audio_host_url': meeting['MediaPlacement']['AudioHostUrl'],
                    'audio_fallback_url': meeting['MediaPlacement']['AudioFallbackUrl'],
                    'signaling_url': meeting['MediaPlacement']['SignalingUrl'],
                    'turn_control_url': meeting['MediaPlacement']['TurnControlUrl'],
                    'screen_data_url': meeting['MediaPlacement'].get('ScreenDataUrl', ''),
                    'screen_viewing_url': meeting['MediaPlacement'].get('ScreenViewingUrl', ''),
                    'screen_sharing_url': meeting['MediaPlacement'].get('ScreenSharingUrl', '')
                }
            }
            
        except ClientError as e:
            logger.error(f"Error creating Chime meeting: {e}")
            raise Exception(f"Failed to create video meeting: {str(e)}")
        except Exception as e:
            logger.error(f"Unexpected error creating meeting: {e}")
            raise
    
    async def create_attendee(
        self,
        meeting_id: str,
        external_user_id: str,
        user_id: str,
        role: str
    ) -> Dict:
        """
        Create an attendee for a Chime meeting
        
        Args:
            meeting_id: AWS Chime meeting ID
            external_user_id: Custom user identifier
            user_id: User's ID from auth service
            role: User role (DOCTOR or PATIENT)
        
        Returns:
            Dict containing attendee details including join token
        """
        try:
            response = self.chime_client.create_attendee(
                MeetingId=meeting_id,
                ExternalUserId=external_user_id,
                Capabilities={
                    'Audio': 'SendReceive',  # Full audio capabilities
                    'Video': 'SendReceive',  # Full video capabilities
                    'Content': 'SendReceive'  # Screen sharing enabled
                },
                Tags=[
                    {'Key': 'UserId', 'Value': user_id},
                    {'Key': 'Role', 'Value': role}
                ]
            )
            
            attendee = response['Attendee']
            
            logger.info(f"Created attendee: {attendee['AttendeeId']} for user: {user_id}")
            
            return {
                'attendee_id': attendee['AttendeeId'],
                'external_user_id': attendee['ExternalUserId'],
                'join_token': attendee['JoinToken'],
                'capabilities': attendee.get('Capabilities', {})
            }
            
        except ClientError as e:
            logger.error(f"Error creating attendee: {e}")
            raise Exception(f"Failed to create meeting attendee: {str(e)}")
        except Exception as e:
            logger.error(f"Unexpected error creating attendee: {e}")
            raise
    
    async def delete_meeting(self, meeting_id: str) -> bool:
        """
        Delete a Chime meeting (ends the meeting for all participants)
        
        Args:
            meeting_id: AWS Chime meeting ID
        
        Returns:
            bool: True if successful
        """
        try:
            self.chime_client.delete_meeting(MeetingId=meeting_id)
            logger.info(f"Deleted Chime meeting: {meeting_id}")
            return True
            
        except ClientError as e:
            if e.response['Error']['Code'] == 'NotFoundException':
                logger.warning(f"Meeting not found: {meeting_id}")
                return True  # Already deleted
            logger.error(f"Error deleting meeting: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error deleting meeting: {e}")
            return False
    
    async def delete_attendee(self, meeting_id: str, attendee_id: str) -> bool:
        """
        Remove an attendee from a meeting
        
        Args:
            meeting_id: AWS Chime meeting ID
            attendee_id: Attendee ID to remove
        
        Returns:
            bool: True if successful
        """
        try:
            self.chime_client.delete_attendee(
                MeetingId=meeting_id,
                AttendeeId=attendee_id
            )
            logger.info(f"Deleted attendee: {attendee_id} from meeting: {meeting_id}")
            return True
            
        except ClientError as e:
            if e.response['Error']['Code'] == 'NotFoundException':
                logger.warning(f"Attendee not found: {attendee_id}")
                return True
            logger.error(f"Error deleting attendee: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error deleting attendee: {e}")
            return False
    
    async def get_meeting(self, meeting_id: str) -> Optional[Dict]:
        """
        Get meeting details
        
        Args:
            meeting_id: AWS Chime meeting ID
        
        Returns:
            Dict with meeting details or None if not found
        """
        try:
            response = self.chime_client.get_meeting(MeetingId=meeting_id)
            return response['Meeting']
            
        except ClientError as e:
            if e.response['Error']['Code'] == 'NotFoundException':
                logger.warning(f"Meeting not found: {meeting_id}")
                return None
            logger.error(f"Error getting meeting: {e}")
            return None
        except Exception as e:
            logger.error(f"Unexpected error getting meeting: {e}")
            return None
    
    async def list_attendees(self, meeting_id: str) -> List[Dict]:
        """
        List all attendees in a meeting
        
        Args:
            meeting_id: AWS Chime meeting ID
        
        Returns:
            List of attendee details
        """
        try:
            attendees = []
            next_token = None
            
            while True:
                if next_token:
                    response = self.chime_client.list_attendees(
                        MeetingId=meeting_id,
                        NextToken=next_token
                    )
                else:
                    response = self.chime_client.list_attendees(MeetingId=meeting_id)
                
                attendees.extend(response.get('Attendees', []))
                
                next_token = response.get('NextToken')
                if not next_token:
                    break
            
            return attendees
            
        except ClientError as e:
            logger.error(f"Error listing attendees: {e}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error listing attendees: {e}")
            return []
    
    async def start_meeting_transcription(
        self,
        meeting_id: str,
        language_code: str = "en-US"
    ) -> bool:
        """
        Start transcription for a meeting (useful for medical records)
        Note: This may incur additional AWS costs
        
        Args:
            meeting_id: AWS Chime meeting ID
            language_code: Language code for transcription
        
        Returns:
            bool: True if successful
        """
        try:
            self.chime_client.start_meeting_transcription(
                MeetingId=meeting_id,
                TranscriptionConfiguration={
                    'EngineTranscribeSettings': {
                        'LanguageCode': language_code,
                        'VocabularyFilterMethod': 'mask',
                        'Region': settings.AWS_REGION
                    }
                }
            )
            logger.info(f"Started transcription for meeting: {meeting_id}")
            return True
            
        except ClientError as e:
            logger.error(f"Error starting transcription: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error starting transcription: {e}")
            return False
    
    async def stop_meeting_transcription(self, meeting_id: str) -> bool:
        """
        Stop transcription for a meeting
        
        Args:
            meeting_id: AWS Chime meeting ID
        
        Returns:
            bool: True if successful
        """
        try:
            self.chime_client.stop_meeting_transcription(MeetingId=meeting_id)
            logger.info(f"Stopped transcription for meeting: {meeting_id}")
            return True
            
        except ClientError as e:
            logger.error(f"Error stopping transcription: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error stopping transcription: {e}")
            return False
    
    def calculate_attendee_minutes(
        self,
        start_time: datetime,
        end_time: Optional[datetime] = None
    ) -> int:
        """
        Calculate attendee-minutes for AWS Free Tier tracking
        
        Args:
            start_time: When attendee joined
            end_time: When attendee left (defaults to now)
        
        Returns:
            int: Number of attendee-minutes used
        """
        if end_time is None:
            end_time = datetime.utcnow()
        
        duration = end_time - start_time
        minutes = int(duration.total_seconds() / 60)
        
        return max(1, minutes)  # Minimum 1 minute


# Global instance
chime_service = ChimeService()
